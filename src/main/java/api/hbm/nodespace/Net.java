package api.hbm.nodespace;

import java.util.*;

import api.hbm.nodespace.Nodespace.Node;
import com.hbm.util.Tuple;
import com.hbm.util.Tuple.Pair;

public class Net {

    public boolean valid = true;
    public Set<Node> links = new HashSet();

    /** Maps all active subscribers to a timestamp, handy for handling timeouts. In a good system this shouldn't be necessary, but the previous system taught me to be cautious anyway */
    public HashMap<INodeReceiver, Long> receiverEntries = new HashMap();
    public HashMap<INodeProvider, Long> providerEntries = new HashMap();

    public NetType netType = NetType.NONE;

    public long tracker = 0L;

    public Net(NetType type) {
        this.netType = type;
        Nodespace.nets.add(this);
    }

    /// SUBSCRIBER HANDLING ///
    public boolean isSubscribed(INodeReceiver receiver) {
        return this.receiverEntries.containsKey(receiver);
    }

    public void addReceiver(INodeReceiver receiver) {
        this.receiverEntries.put(receiver, System.currentTimeMillis());
    }

    public void removeReceiver(INodeReceiver receiver) {
        this.receiverEntries.remove(receiver);
    }

    /// PROVIDER HANDLING ///
    public boolean isProvider(INodeProvider provider) {
        return this.providerEntries.containsKey(provider);
    }

    public void addProvider(INodeProvider provider) {
        this.providerEntries.put(provider, System.currentTimeMillis());
    }

    public void removeProvider(INodeProvider provider) {
        this.providerEntries.remove(provider);
    }

    /// LINK JOINING ///

    /** Combines two networks into one */
    public void joinNetworks(Net network) {

        if(network == this) return; //wtf?!

        // if the net types are *not* the same, then do not merge them (merging a power and a fluid net could cause some... undesirable consequences)
        if(this.netType != network.netType) return;

        List<Node> oldNodes = new ArrayList(network.links.size());
        oldNodes.addAll(network.links); // might prevent oddities related to joining - nvm it does nothing

        for(Node conductor : oldNodes) forceJoinLink(conductor);
        network.links.clear();

        for(INodeReceiver connector : network.receiverEntries.keySet()) this.addReceiver(connector);
        for(INodeProvider connector : network.providerEntries.keySet()) this.addProvider(connector);
        network.destroy();
    }

    /** Adds the node as part of this network's links */
    public Net joinLink(Node node) {
        if(node.net != null) node.net.leaveLink(node);
        return forceJoinLink(node);
    }

    /** Adds the amount node as part of this network's links, skips the part about removing it from existing networks. */
    public Net forceJoinLink(Node node) {
        this.links.add(node);
        node.setNet(this);
        return this;
    }

    /** Removes the specified node */
    public void leaveLink(Node node) {
        node.setNet(null);
        this.links.remove(node);
    }

    /// GENERAL NET CONTROL ///
    public void invalidate() {
        this.valid = false;
        Nodespace.nets.remove(this);
    }

    public boolean isValid() {
        return this.valid;
    }

    public void destroy() {
        this.invalidate();
        for(Node link : this.links) if(link.net == this) link.setNet(null);
        this.links.clear();
        this.receiverEntries.clear();
        this.providerEntries.clear();
    }

    public void resetTracker() {
        this.tracker = 0;
    }

    protected static int timeout = 3_000;

    public void transfer() {

        if(providerEntries.isEmpty()) return;
        if(receiverEntries.isEmpty()) return;

        long timestamp = System.currentTimeMillis();

        List<Tuple.Pair<INodeProvider, Long>> providers = new ArrayList();
        long amountAvailable = 0;

        Iterator<Map.Entry<INodeProvider, Long>> provIt = providerEntries.entrySet().iterator();
        while(provIt.hasNext()) {
            Map.Entry<INodeProvider, Long> entry = provIt.next();
            if(timestamp - entry.getValue() > timeout) { provIt.remove(); continue; }
            long src = Math.min(entry.getKey().getAmount(), entry.getKey().getProviderSpeed());
            providers.add(new Tuple.Pair(entry.getKey(), src));
            amountAvailable += src;
        }

        List<Tuple.Pair<INodeReceiver, Long>>[] receivers = new ArrayList[INodeReceiver.ConnectionPriority.values().length];
        for(int i = 0; i < receivers.length; i++) receivers[i] = new ArrayList();
        long[] demand = new long[INodeReceiver.ConnectionPriority.values().length];
        long totalDemand = 0;

        Iterator<Map.Entry<INodeReceiver, Long>> recIt = receiverEntries.entrySet().iterator();

        while(recIt.hasNext()) {
            Map.Entry<INodeReceiver, Long> entry = recIt.next();
            if(timestamp - entry.getValue() > timeout) { recIt.remove(); continue; }
            long rec = Math.min(entry.getKey().getMaxAmount() - entry.getKey().getAmount(), entry.getKey().getReceiverSpeed());
            int p = entry.getKey().getPriority().ordinal();
            receivers[p].add(new Tuple.Pair(entry.getKey(), rec));
            demand[p] += rec;
            totalDemand += rec;
        }

        long toTransfer = Math.min(amountAvailable, totalDemand);
        long amountUsed = 0;

        for(int i = INodeReceiver.ConnectionPriority.values().length - 1; i >= 0; i--) {
            List<Tuple.Pair<INodeReceiver, Long>> list = receivers[i];
            long priorityDemand = demand[i];

            for(Tuple.Pair<INodeReceiver, Long> entry : list) {
                double weight = (double) entry.getValue() / (double) (priorityDemand);
                long toSend = (long) Math.max(toTransfer * weight, 0D);
                amountUsed += (toSend - entry.getKey().transfer(toSend)); //leftovers are subtracted from the intended amount to use up
            }

            toTransfer -= amountUsed;
        }

        this.tracker += amountUsed;

        for(Tuple.Pair<INodeProvider, Long> entry : providers) {
            double weight = (double) entry.getValue() / (double) amountAvailable;
            long toUse = (long) Math.max(amountUsed * weight, 0D);
            entry.getKey().use(toUse);
        }
    }

    @Deprecated public void transferOld() {

        if(providerEntries.isEmpty()) return;
        if(receiverEntries.isEmpty()) return;

        long timestamp = System.currentTimeMillis();
        long transferCap = 100_000_000_000_000_00L; // that ought to be enough

        long supply = 0;
        long demand = 0;
        long[] priorityDemand = new long[INodeReceiver.ConnectionPriority.values().length];

        Iterator<Map.Entry<INodeProvider, Long>> provIt = providerEntries.entrySet().iterator();
        while(provIt.hasNext()) {
            Map.Entry<INodeProvider, Long> entry = provIt.next();
            if(timestamp - entry.getValue() > timeout) { provIt.remove(); continue; }
            supply += Math.min(entry.getKey().getAmount(), entry.getKey().getProviderSpeed());
        }

        if(supply <= 0) return;

        Iterator<Map.Entry<INodeReceiver, Long>> recIt = receiverEntries.entrySet().iterator();
        while(recIt.hasNext()) {
            Map.Entry<INodeReceiver, Long> entry = recIt.next();
            if(timestamp - entry.getValue() > timeout) { recIt.remove(); continue; }
            long rec = Math.min(entry.getKey().getMaxAmount() - entry.getKey().getAmount(), entry.getKey().getReceiverSpeed());
            demand += rec;
            for(int i = 0; i <= entry.getKey().getPriority().ordinal(); i++) priorityDemand[i] += rec;
        }

        if(demand <= 0) return;

        long toTransfer = Math.min(supply, demand);
        if(toTransfer > transferCap) toTransfer = transferCap;
        if(toTransfer <= 0) return;

        List<INodeProvider> buffers = new ArrayList();
        List<INodeProvider> providers = new ArrayList();
        Set<INodeReceiver> receiverSet = receiverEntries.keySet();
        for(INodeProvider provider : providerEntries.keySet()) {
            if(receiverSet.contains(provider)) {
                buffers.add(provider);
            } else {
                providers.add(provider);
            }
        }
        providers.addAll(buffers); //makes buffers go last
        List<INodeReceiver> receivers = new ArrayList() {{ addAll(receiverSet); }};

        receivers.sort(COMP);

        int maxIteration = 1000;

        //how much the current sender/receiver have already sent/received
        long prevSrc = 0;
        long prevDest = 0;

        while(!receivers.isEmpty() && !providers.isEmpty() && maxIteration > 0) {
            maxIteration--;

            INodeProvider src = providers.get(0);
            INodeReceiver dest = receivers.get(0);

            if(src.getAmount() <= 0) { providers.remove(0); prevSrc = 0; continue; }

            if(src == dest) { // STALEMATE DETECTED
                //if this happens, a buffer will waste both its share of transfer and receiving potential and do effectively nothing, essentially breaking

                //try if placing the conflicting provider at the end of the list does anything
                //we do this first because providers have no priority, so we may shuffle those around as much as we want
                if(providers.size() > 1) {
                    providers.add(providers.get(0));
                    providers.remove(0);
                    prevSrc = 0; //this might cause slight issues due to the tracking being effectively lost while there still might be pending operations
                    continue;
                }
                //if that didn't work, try shifting the receiver by one place (to minimize priority breakage)
                if(receivers.size() > 1) {
                    receivers.add(2, receivers.get(0));
                    receivers.remove(0);
                    prevDest = 0; //ditto
                    continue;
                }

                //if neither option could be performed, the only conclusion is that this buffer mode battery is alone in the amount net, in which case: not my provlem
            }

            long pd = priorityDemand[dest.getPriority().ordinal()];

            long receiverShare = Math.min((long) Math.ceil((double) Math.min(dest.getMaxAmount() - dest.getAmount(), dest.getReceiverSpeed()) * (double) supply / (double) pd), dest.getReceiverSpeed()) - prevDest;
            long providerShare = Math.min((long) Math.ceil((double) Math.min(src.getAmount(), src.getProviderSpeed()) * (double) demand / (double) supply), src.getProviderSpeed()) - prevSrc;

            long toDrain = Math.min((long) (src.getAmount()), providerShare);
            long toFill = Math.min(dest.getMaxAmount() - dest.getAmount(), receiverShare);

            long finalTransfer = Math.min(toDrain, toFill);
            if(toFill <= 0) { receivers.remove(0); prevDest = 0; continue; }

            finalTransfer -= dest.transfer(finalTransfer);
            src.use(finalTransfer);

            prevSrc += finalTransfer;
            prevDest += finalTransfer;

            if(prevSrc >= src.getProviderSpeed()) { providers.remove(0); prevSrc = 0; continue; }
            if(prevDest >= dest.getReceiverSpeed()) { receivers.remove(0); prevDest = 0; continue; }

            toTransfer -= finalTransfer;
            this.tracker += finalTransfer;
        }
    }

    public long sendDiode(long amount) {

        if(receiverEntries.isEmpty()) return amount;

        long timestamp = System.currentTimeMillis();

        List<Tuple.Pair<INodeReceiver, Long>>[] receivers = new ArrayList[INodeReceiver.ConnectionPriority.values().length];
        for(int i = 0; i < receivers.length; i++) receivers[i] = new ArrayList();
        long[] demand = new long[INodeReceiver.ConnectionPriority.values().length];
        long totalDemand = 0;

        Iterator<Map.Entry<INodeReceiver, Long>> recIt = receiverEntries.entrySet().iterator();

        while(recIt.hasNext()) {
            Map.Entry<INodeReceiver, Long> entry = recIt.next();
            if(timestamp - entry.getValue() > timeout) { recIt.remove(); continue; }
            long rec = Math.min(entry.getKey().getMaxAmount() - entry.getKey().getAmount(), entry.getKey().getReceiverSpeed());
            int p = entry.getKey().getPriority().ordinal();
            receivers[p].add(new Tuple.Pair(entry.getKey(), rec));
            demand[p] += rec;
            totalDemand += rec;
        }

        long toTransfer = Math.min(amount, totalDemand);
        long amountUsed = 0;

        for(int i = INodeReceiver.ConnectionPriority.values().length - 1; i >= 0; i--) {
            List<Tuple.Pair<INodeReceiver, Long>> list = receivers[i];
            long priorityDemand = demand[i];

            for(Tuple.Pair<INodeReceiver, Long> entry : list) {
                double weight = (double) entry.getValue() / (double) (priorityDemand);
                long toSend = (long) Math.max(toTransfer * weight, 0D);
                amountUsed += (toSend - entry.getKey().transfer(toSend)); //leftovers are subtracted from the intended amount to use up
            }

            toTransfer -= amountUsed;
        }

        this.tracker += amountUsed;

        return amount - amountUsed;
    }

    public static final Net.ReceiverComparator COMP = new Net.ReceiverComparator();

    public static class ReceiverComparator implements Comparator<INodeReceiver> {

        @Override
        public int compare(INodeReceiver o1, INodeReceiver o2) {
            return o2.getPriority().ordinal() - o1.getPriority().ordinal();
        }
    }

    public enum NetType {
        NONE,
        ENERGY,
        FLUID
    }
}
