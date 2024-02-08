package no.nav.foreldrepenger.dokumentbestiller.infobrev;

import java.time.LocalDate;

import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.RelasjonsRolleType;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.Saksnummer;

public class OverlappData {
    private Saksnummer saksnummer;
    private Long behandlingId;
    private FagsakYtelseType ytelseType;
    private RelasjonsRolleType rolle;
    private AktørId aktørId;
    private LocalDate tidligsteDato;

    public Saksnummer getSaksnummer() {
        return saksnummer;
    }

    public FagsakYtelseType getYtelseType() {
        return ytelseType;
    }

    public RelasjonsRolleType getRolle() {
        return rolle;
    }

    public AktørId getAktørId() {
        return aktørId;
    }

    public LocalDate getTidligsteDato() {
        return tidligsteDato;
    }

    public Long getBehandlingId() {
        return behandlingId;
    }

    public static class OverlappDataBuilder {
        private final OverlappData data;

        OverlappDataBuilder(OverlappData data) {
            this.data = data;
        }

        public static OverlappDataBuilder ny() {
            return new OverlappDataBuilder(new OverlappData());
        }

        public OverlappDataBuilder medSaksnummer(String saksnummer) {
            this.data.saksnummer = new Saksnummer(saksnummer);
            return this;
        }

        public OverlappDataBuilder medBehandlingId(Long id) {
            this.data.behandlingId = id;
            return this;
        }

        public OverlappDataBuilder medYtelseType(String ytelseType) {
            this.data.ytelseType = FagsakYtelseType.fraKode(ytelseType);
            return this;
        }

        public OverlappDataBuilder medAktørId(String aktør) {
            this.data.aktørId = new AktørId(aktør);
            return this;
        }

        public OverlappDataBuilder medTidligsteDato(LocalDate første) {
            this.data.tidligsteDato = første;
            return this;
        }

        public OverlappDataBuilder medRolle(String rolle) {
            this.data.rolle = rolle == null ? RelasjonsRolleType.UDEFINERT : RelasjonsRolleType.fraKode(rolle);
            return this;
        }

        public OverlappData build() {
            return this.data;
        }
    }
}
