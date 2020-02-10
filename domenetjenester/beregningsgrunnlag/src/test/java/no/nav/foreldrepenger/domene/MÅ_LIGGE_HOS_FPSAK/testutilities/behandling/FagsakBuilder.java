package no.nav.foreldrepenger.domene.MÅ_LIGGE_HOS_FPSAK.testutilities.behandling;

import no.nav.foreldrepenger.behandlingslager.aktør.NavBruker;
import no.nav.foreldrepenger.behandlingslager.aktør.NavBrukerKjønn;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.RelasjonsRolleType;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.testutilities.aktør.NavBrukerBuilder;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.Saksnummer;

/**
 * Builder for å bygge enkle fagsaker. Primært for test.
 */
public class FagsakBuilder {

    private Saksnummer saksnummer;

    private NavBrukerBuilder brukerBuilder = new NavBrukerBuilder();

    private RelasjonsRolleType rolle;

    private Fagsak fagsak;

    private FagsakYtelseType fagsakYtelseType;

    private FagsakBuilder(RelasjonsRolleType rolle, FagsakYtelseType fagsakYtelseType) {
        this.rolle = rolle;
        this.fagsakYtelseType = fagsakYtelseType;
    }

    private FagsakBuilder(Fagsak fagsak) {
        this.fagsak = fagsak;
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

    public FagsakBuilder medBrukerAktørId(AktørId aktørId) {
        validerFagsakIkkeSatt();
        brukerBuilder.medAktørId(aktørId);
        return this;
    }

    public FagsakBuilder medBrukerKjønn(NavBrukerKjønn kjønn) {
        validerFagsakIkkeSatt();
        brukerBuilder.medKjønn(kjønn);
        return this;
    }

    public NavBrukerBuilder getBrukerBuilder() {
        return brukerBuilder;
    }

    public RelasjonsRolleType getRolle() {
        return rolle;
    }

    public FagsakBuilder medBruker(NavBruker bruker) {
        validerFagsakIkkeSatt();
        brukerBuilder.medBruker(bruker);
        return this;
    }

    public static FagsakBuilder nyFagsak(FagsakYtelseType fagsakYtelseType, RelasjonsRolleType rolle) {
        return new FagsakBuilder(rolle, fagsakYtelseType);
    }

    public Fagsak build() {

        if (fagsak != null) {
            return fagsak;
        } else {
            fagsak = Fagsak.opprettNy(fagsakYtelseType, brukerBuilder.build(), rolle, saksnummer);
            return fagsak;
        }

    }
}
