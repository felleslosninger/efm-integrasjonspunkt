package no.difi.meldingsutveksling.queue.domain;

import no.difi.meldingsutveksling.queue.rule.Rule;
import no.difi.meldingsutveksling.queue.rule.RuleDefault;

import java.util.Date;

public class Queue {
    private String uniqueId;
    private int numberAttempt;
    private Rule rule;
    private Date lastAttemptTime;
    private String location;
    private String checksum;
    private Status status;

    private Queue(String uniqueId, int numberAttempt, Rule rule, Date lastAttemptTime, String location, String checksum, Status status) {
        this.uniqueId = uniqueId;
        this.numberAttempt = numberAttempt;
        this.rule = rule == null ? RuleDefault.getRule() : rule;
        this.lastAttemptTime = lastAttemptTime;
        this.location = checkBlank(location);
        this.checksum = checkBlank(checksum);
        this.status = status == null ? Status.NEW : status;
    }

    public String getUniqueId() {
        return uniqueId;
    }

    private int getNextAttempt() {
        return rule.getMinutesToNextAttempt(++numberAttempt);
    }

    public int getNumberAttempts() {
        return numberAttempt;
    }

    public Rule getRule() {
        return rule;
    }

    public String getRuleName() {
        return rule.getClass().getName();
    }

    public Status getStatus() {
        return status;
    }

    public String getStatusName() {
        return status.name();
    }

    public String getFileLocation() {
        return location;
    }

    public Date getLastAttemptTime() {
        return lastAttemptTime;
    }

    public String getChecksum() {
        return checksum;
    }

    public Builder getOpenObjectBuilder() {
        return new Builder()
                .uniqueId(this.uniqueId)
                .numberAttempt(this.numberAttempt)
                .rule(this.rule)
                .lastAttemptTime(this.lastAttemptTime)
                .location(this.location)
                .checksum(this.checksum)
                .status(this.status);
    }

    public static class Builder {
        private String uniqueId;
        private int numberAttempt;
        private Rule rule;
        private Date lastAttemptTime;
        private String location;
        private String checksum;
        private Status status;

        public Builder uniqueId(String uniqueId) {
            this.uniqueId = uniqueId;
            return this;
        }

        public Builder numberAttempt(int attempt) {
            this.numberAttempt = attempt;
            return this;
        }

        public Builder rule(Rule rule) {
            this.rule = rule;
            return this;
        }

        public Builder rule(String rule) {
            if (rule.contains("RuleDefault")) {
                this.rule = RuleDefault.getRule();
            }
            return this;
        }

        public Builder lastAttemptTime(Date lastAttemptTime) {
            this.lastAttemptTime = lastAttemptTime;
            return this;
        }

        public Builder location(String location) {
            this.location = location;
            return this;
        }

        public Builder checksum(String checksum) {
            this.checksum = checksum;
            return this;
        }

        public Builder status(Status status) {
            this.status = status;
            return this;
        }

        public Builder status(String status) {
            this.status = Status.valueOf(status);
            return this;
        }

        public Queue build() {
            return new Queue(uniqueId, numberAttempt, rule, lastAttemptTime, location, checksum, status);
        }
    }

    private String checkBlank(String value) {
        return value == null ? "" : value;
    }
}
