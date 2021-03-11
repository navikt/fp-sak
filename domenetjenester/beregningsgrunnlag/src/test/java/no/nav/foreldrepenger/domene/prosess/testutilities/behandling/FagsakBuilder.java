package no.nav.foreldrepenger.domene.prosess.testutilities.behandling;

import no.nav.foreldrepenger.behandlingslager.aktør.NavBrukerKjønn;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.RelasjonsRolleType;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.testutilities.aktør.NavBrukerBuilder;
import no.nav.foreldrepenger.domene.typer.Saksnummer;

/**
 * Builder for å bygge enkle fagsaker. Primært for test.
 */
public class FagsakBuilder {

    private final NavBrukerBuilder brukerBuilder = new NavBrukerBuilder();
    private final RelasjonsRolleType rolle;
    private final FagsakYtelseType fagsakYtelseType;

    private Saksnummer saksnummer;
    private Fagsak fagsak;

    private FagsakBuilder(RelasjonsRolleType rolle, FagsakYtelseType fagsakYtelseType) {
        this.rolle = rolle;
        this.fagsakYtelseType = fagsakYtelseType;
    }

    public FagsakBuilder medSaksnummer(Saksnummer saksnummer) {
        validerFagsakIkkeSatt();
        this.saksnummer = saksnummer;
        return this;
    }

    private void validerFagsakIkkeSatt() {
        if (fagsak != null) {
            throw new IllegalStateException("Fagsak er allerede konfigurert, kan ikke overstyre her");
        }
    }

    public FagsakBuilder medBrukerKjønn(NavBrukerKjønn kjønn) {
        validerFagsakIkkeSatt();
        brukerBuilder.medKjønn(kjønn);
        return this;
    }

    public static FagsakBuilder nyFagsak(FagsakYtelseType fagsakYtelseType, RelasjonsRolleType rolle) {
        return new FagsakBuilder(rolle, fagsakYtelseType);
    }

    public Fagsak build() {
        if (fagsak == null) {
            fagsak = Fagsak.opprettNy(fagsakYtelseType, brukerBuilder.build(), rolle, saksnummer);
        }
        return fagsak;

    }
}
