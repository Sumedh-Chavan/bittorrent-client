package bittorrentClient.tracker;

import bittorrentClient.peer.PeerInfo;

import java.util.ArrayList;
import java.util.List;

public class TrackerResponse {
        private String failureReason;
        private String warningMessage;
        private Long interval;
        private Long minInterval;
        private String trackerId;
        private Long complete;
        private Long incomplete;

        // Optional: only one of these is expected
        private List<PeerInfo> peers;      // For dictionary model
        private byte[] binaryPeers;        // For compact/binary model

        public TrackerResponse()
        {
            peers = new ArrayList<PeerInfo>();
        }
        // Getters and setters

        public String getFailureReason() {
            return failureReason;
        }

        public void setFailureReason(String failureReason) {
            this.failureReason = failureReason;
        }

        public String getWarningMessage() {
            return warningMessage;
        }

        public void setWarningMessage(String warningMessage) {
            this.warningMessage = warningMessage;
        }

        public Long getInterval() {
            return interval;
        }

        public void setInterval(Long interval) {
            this.interval = interval;
        }

        public Long getMinInterval() {
            return minInterval;
        }

        public void setMinInterval(Long minInterval) {
            this.minInterval = minInterval;
        }

        public String getTrackerId() {
            return trackerId;
        }

        public void setTrackerId(String trackerId) {
            this.trackerId = trackerId;
        }

        public Long getComplete() {
            return complete;
        }

        public void setComplete(Long complete) {
            this.complete = complete;
        }

        public Long getIncomplete() {
            return incomplete;
        }

        public void setIncomplete(Long incomplete) {
            this.incomplete = incomplete;
        }

        public List<PeerInfo> getPeers() {
            return peers;
        }

        public void setPeers(List<PeerInfo> peers) {
            this.peers = peers;
        }

        public byte[] getBinaryPeers() {
            return binaryPeers;
        }

        public void setBinaryPeers(byte[] binaryPeers) {
            this.binaryPeers = binaryPeers;
        }

        @Override
        public String toString() {
            return "TrackerResponse{" +
                    "failureReason='" + failureReason + '\'' +
                    ", warningMessage='" + warningMessage + '\'' +
                    ", interval=" + interval +
                    ", minInterval=" + minInterval +
                    ", trackerId='" + trackerId + '\'' +
                    ", complete=" + complete +
                    ", incomplete=" + incomplete +
                    ", peers=" + peers +
                    ", binaryPeers=" + (binaryPeers != null ? binaryPeers.length + " bytes" : "null") +
                    '}';
        }
    }

