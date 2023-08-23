package no.nav.foreldrepenger.behandlingsprosess.dagligejobber.infobrev;

import no.nav.foreldrepenger.domene.typer.AktørId;

import java.time.LocalDate;

public class InformasjonssakData {
    private Long kildeFagsakId;
    private AktørId aktørId;
    private LocalDate kildesakOpprettetDato;
    private LocalDate familieHndelseDato;
    private String enhet;

    InformasjonssakData(Long sak) {
        this.kildeFagsakId = sak;
    }

    public Long getKildeFagsakId() {
        return kildeFagsakId;
    }

    public AktørId getAktørId() {
        return aktørId;
    }

    public LocalDate getKildesakOpprettetDato() {
        return kildesakOpprettetDato;
    }

    public LocalDate getFamilieHndelseDato() {
        return familieHndelseDato;
    }

    public String getEnhet() {
        return enhet;
    }

    public static class InformasjonssakDataBuilder {
        private final InformasjonssakData data;

        InformasjonssakDataBuilder(InformasjonssakData data) {
            this.data = data;
        }

        public static InformasjonssakDataBuilder ny(Long fagsakId) {
            return new InformasjonssakDataBuilder(new InformasjonssakData(fagsakId));
        }

        public InformasjonssakDataBuilder medAktørIdAnnenPart(String aktør) {
            this.data.aktørId = new AktørId(aktør);
            return this;
        }

        public InformasjonssakDataBuilder medOpprettetDato(LocalDate opprettet) {
            this.data.kildesakOpprettetDato = opprettet;
            return this;
        }

        public InformasjonssakDataBuilder medHendelseDato(LocalDate hendelse) {
            this.data.familieHndelseDato = hendelse;
            return this;
        }

        public InformasjonssakDataBuilder medEnhet(String enhet) {
            this.data.enhet = enhet;
            return this;
        }

        public InformasjonssakData build() {
            return this.data;
        }
    }
}
