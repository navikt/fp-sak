package no.nav.foreldrepenger.domene.feed;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

@Entity(name = "FpVedtakUtgåendeHendelse")
@DiscriminatorValue(FpVedtakUtgåendeHendelse.FEED_NAVN_VEDTAK)
@SekvensnummerNavn(value= FpVedtakUtgåendeHendelse.SEQ_GENERATOR_NAVN)
public class FpVedtakUtgåendeHendelse extends UtgåendeHendelse {

    static final String FEED_NAVN_VEDTAK = "VEDTAK_FP";
    static final String SEQ_GENERATOR_NAVN = "SEQ_" + FEED_NAVN_VEDTAK;

    private FpVedtakUtgåendeHendelse() {
        super();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String type;
        private String payload;
        private String aktørId;
        private String kildeId;

        public Builder type(String type) {
            this.type = type;
            return this;
        }

        public Builder payload(String payload) {
            this.payload = payload;
            return this;
        }

        public Builder aktørId(String aktørId) {
            this.aktørId = aktørId;
            return this;
        }

        public Builder kildeId(String kildeId) {
            this.kildeId = kildeId;
            return this;
        }

        public FpVedtakUtgåendeHendelse build() {
            var hendelse = new FpVedtakUtgåendeHendelse();
            hendelse.setType(type);
            hendelse.setPayload(payload);
            hendelse.setAktørId(aktørId);
            hendelse.setKildeId(kildeId);
            return hendelse;
        }
    }
}
