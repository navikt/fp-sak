package no.nav.foreldrepenger.domene.feed;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

@Entity(name = "SvpVedtakUtgåendeHendelse")
@DiscriminatorValue(SvpVedtakUtgåendeHendelse.FEED_NAVN_VEDTAK)
@SekvensnummerNavn(value = SvpVedtakUtgåendeHendelse.SEQ_GENERATOR_NAVN)
public class SvpVedtakUtgåendeHendelse extends UtgåendeHendelse {

    static final String FEED_NAVN_VEDTAK = "VEDTAK_SVP";
    static final String SEQ_GENERATOR_NAVN = "SEQ_" + FEED_NAVN_VEDTAK;

    private SvpVedtakUtgåendeHendelse() {
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

        public SvpVedtakUtgåendeHendelse build() {
            var hendelse = new SvpVedtakUtgåendeHendelse();
            hendelse.setType(type);
            hendelse.setPayload(payload);
            hendelse.setAktørId(aktørId);
            hendelse.setKildeId(kildeId);
            return hendelse;
        }
    }
}
