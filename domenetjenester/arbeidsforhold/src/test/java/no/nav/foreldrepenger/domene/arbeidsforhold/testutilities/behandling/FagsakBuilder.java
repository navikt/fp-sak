package no.nav.foreldrepenger.domene.arbeidsforhold.testutilities.behandling;

import no.nav.foreldrepenger.behandlingslager.aktør.NavBruker;
import no.nav.foreldrepenger.behandlingslager.aktør.NavBrukerKjønn;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.RelasjonsRolleType;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.Saksnummer;

/**
 * Builder for å bygge enkle fagsaker. Primært for test.
 */
class FagsakBuilder {

    private Saksnummer saksnummer;

    private NavBrukerBuilder brukerBuilder = new NavBrukerBuilder();

    private FagsakRelasjonBuilder fagsakRelasjonBuilder;
    private RelasjonsRolleType rolle;

    private Fagsak fagsak;

    private FagsakBuilder(RelasjonsRolleType rolle, FagsakRelasjonBuilder fagsakRelasjonBuilder) {
        this.rolle = rolle;
        this.fagsakRelasjonBuilder = fagsakRelasjonBuilder;
    }

    FagsakBuilder medSaksnummer(Saksnummer saksnummer) {
        validerFagsakIkkeSatt();
        this.saksnummer = saksnummer;
        return this;
    }

    private void validerFagsakIkkeSatt() {
        if (fagsak != null) {
            throw new IllegalStateException("Fagsak er allerede konfigurert, kan ikke overstyre her");
        }
    }

    FagsakBuilder medBrukerAktørId(AktørId aktørId) {
        validerFagsakIkkeSatt();
        brukerBuilder.medAktørId(aktørId);
        return this;
    }

    FagsakBuilder medBrukerKjønn(NavBrukerKjønn kjønn) {
        validerFagsakIkkeSatt();
        brukerBuilder.medKjønn(kjønn);
        return this;
    }

    NavBrukerBuilder getBrukerBuilder() {
        return brukerBuilder;
    }

    RelasjonsRolleType getRolle() {
        return rolle;
    }

    FagsakBuilder medBruker(NavBruker bruker) {
        validerFagsakIkkeSatt();
        brukerBuilder.medBruker(bruker);
        return this;
    }

    static FagsakBuilder nyFagsak(FagsakYtelseType fagsakYtelseType, RelasjonsRolleType rolle) {
        if (fagsakYtelseType.equals(FagsakYtelseType.ENGANGSTØNAD)) {
            return nyEngangstønad(rolle);
        }
        if (fagsakYtelseType.equals(FagsakYtelseType.FORELDREPENGER)) {
            return nyForeldrepengesak(rolle);
        }
        throw new IllegalStateException("Utviklerfeil: Kan ikke opprette fagsak for udefinert FagsakYtelseType");
    }

    private static FagsakBuilder nyEngangstønad(RelasjonsRolleType rolle) {
        return new FagsakBuilder(rolle, FagsakRelasjonBuilder.engangsstønad());
    }

    private static FagsakBuilder nyForeldrepengesak(RelasjonsRolleType rolle) {
        return new FagsakBuilder(rolle, FagsakRelasjonBuilder.foreldrepenger());
    }

    Fagsak build() {

        if (fagsak != null) {
            return fagsak;
        }
        fagsak = Fagsak.opprettNy(fagsakRelasjonBuilder.getYtelseType(), brukerBuilder.build(), rolle, saksnummer);
        return fagsak;

    }
}
