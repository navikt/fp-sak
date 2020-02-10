package no.nav.foreldrepenger.domene.person.verge;

import java.util.Optional;

import no.nav.foreldrepenger.behandlingslager.aktør.NavBruker;
import no.nav.foreldrepenger.behandlingslager.aktør.NavBrukerRepository;
import no.nav.foreldrepenger.behandlingslager.aktør.Personinfo;
import no.nav.foreldrepenger.behandlingslager.behandling.verge.VergeBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.verge.VergeOrganisasjonBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.verge.VergeOrganisasjonEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.verge.VergeRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.verge.VergeType;
import no.nav.foreldrepenger.domene.person.tps.TpsAdapter;
import no.nav.foreldrepenger.domene.person.verge.dto.VergeAksjonpunktDto;
import no.nav.foreldrepenger.domene.personopplysning.OppdatererAksjonspunktFeil;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.PersonIdent;

public class VergeOppdatererAksjonspunkt {
    private TpsAdapter tpsAdapter;
    private NavBrukerRepository navBrukerRepository;
    private VergeRepository vergeRepository;

    public VergeOppdatererAksjonspunkt(VergeRepository vergeRepository, TpsAdapter tpsAdapter, NavBrukerRepository navBrukerRepository) {
        this.tpsAdapter = tpsAdapter;
        this.navBrukerRepository = navBrukerRepository;
        this.vergeRepository = vergeRepository;
    }

    public void oppdater(Long behandlingId, VergeAksjonpunktDto adapter) {
        VergeBuilder vergeBuilder = new VergeBuilder()
            .gyldigPeriode(adapter.getFom(), adapter.getTom())
            .medVergeType(VergeType.fraKode(adapter.getVergeTypeKode()));

        // Verge må enten være oppgitt med fnr (hent ut fra TPS), eller orgnr
        PersonIdent fnr = adapter.getFnr();
        if (fnr != null) {
            Optional<AktørId> optAktorId = tpsAdapter.hentAktørIdForPersonIdent(fnr);
            optAktorId.ifPresent(aktørId -> vergeBuilder.medBruker(hentEllerOpprettBruker(fnr, aktørId)));
        } else if (adapter.getOrganisasjonsnummer() != null) {
            vergeBuilder.medVergeOrganisasjon(opprettVergeOrganisasjon(adapter));
        } else {
            throw OppdatererAksjonspunktFeil.FACTORY.vergeIkkeFunnetITPS().toException();
        }

        vergeRepository.lagreOgFlush(behandlingId, vergeBuilder);
    }

    private NavBruker hentEllerOpprettBruker(PersonIdent fnr, AktørId aktoerId) {
        Optional<NavBruker> optBruker = navBrukerRepository.hent(aktoerId);

        if (optBruker.isPresent()) {
            return optBruker.get();
        } else {
            Personinfo personinfo = tpsAdapter.hentKjerneinformasjon(fnr, aktoerId);
            return NavBruker.opprettNy(personinfo);
        }
    }

    private VergeOrganisasjonEntitet opprettVergeOrganisasjon(VergeAksjonpunktDto adapter) {
        return new VergeOrganisasjonBuilder()
            .medOrganisasjonsnummer(adapter.getOrganisasjonsnummer())
            .medNavn(adapter.getNavn())
            .build();
    }
}
