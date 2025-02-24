package no.nav.foreldrepenger.domene.person.verge;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.aktør.NavBruker;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkAktør;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.Historikkinnslag;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.skjermlenke.SkjermlenkeType;
import no.nav.foreldrepenger.behandlingslager.behandling.verge.*;
import no.nav.foreldrepenger.domene.bruker.NavBrukerTjeneste;
import no.nav.foreldrepenger.domene.person.PersoninfoAdapter;
import no.nav.foreldrepenger.domene.person.verge.dto.OpprettVergeDto;
import no.nav.foreldrepenger.domene.typer.PersonIdent;

@ApplicationScoped
public class OpprettVergeTjeneste {

    private PersoninfoAdapter personinfoAdapter;
    private NavBrukerTjeneste brukerTjeneste;
    private VergeRepository vergeRepository;
    private HistorikkinnslagRepository historikkinnslagRepository;

    @Inject
    public OpprettVergeTjeneste(PersoninfoAdapter personinfoAdapter,
                                NavBrukerTjeneste brukerTjeneste,
                                VergeRepository vergeRepository,
                                HistorikkinnslagRepository historikkinnslagRepository) {
        this.personinfoAdapter = personinfoAdapter;
        this.brukerTjeneste = brukerTjeneste;
        this.vergeRepository = vergeRepository;
        this.historikkinnslagRepository = historikkinnslagRepository;
    }

    OpprettVergeTjeneste() {
        // CDI
    }

    public void opprettVerge(Long behandlingId, Long fagsakId, OpprettVergeDto dto) {
        var vergeBuilder = new VergeEntitet.Builder().gyldigPeriode(dto.gyldigFom(), dto.gyldigTom()).medVergeType(dto.vergeType());

        if (VergeType.ADVOKAT.equals(dto.vergeType())) {
            vergeBuilder.medVergeOrganisasjon(opprettVergeOrganisasjon(dto));
        } else {
            var personIdent = new PersonIdent(dto.fnr());
            vergeBuilder.medBruker(hentEllerOpprettBruker(personIdent));
        }
        var harEksisterendeVerge = vergeRepository.hentAggregat(behandlingId).isPresent();

        vergeRepository.lagreOgFlush(behandlingId, vergeBuilder);
        opprettHistorikkinnslag(behandlingId, fagsakId, dto, harEksisterendeVerge);
    }

    private NavBruker hentEllerOpprettBruker(PersonIdent personIdent) {

        return personinfoAdapter.hentAktørForFnr(personIdent)
            .map(vergeAktorId -> brukerTjeneste.hentEllerOpprettFraAktørId(vergeAktorId))
            .orElseThrow(() -> new IllegalArgumentException("Ugyldig FNR for Verge"));
    }

    private VergeOrganisasjonEntitet opprettVergeOrganisasjon(OpprettVergeDto dto) {
        return new VergeOrganisasjonEntitet.Builder().medOrganisasjonsnummer(dto.organisasjonsnummer()).medNavn(dto.navn()).build();
    }

    private void opprettHistorikkinnslag(Long behandlingId, Long fagsakId, OpprettVergeDto dto, boolean erEndring) {
        var builder = new Historikkinnslag.Builder().medFagsakId(fagsakId)
            .medBehandlingId(behandlingId)
            .medAktør(HistorikkAktør.SAKSBEHANDLER)
            .medTittel(SkjermlenkeType.FAKTA_OM_VERGE)
            .addLinje(String.format("Opplysninger om verge/fullmektig er %s.", erEndring ? "endret" : "registrert"))
            .addLinje(dto.begrunnelse());

        historikkinnslagRepository.lagre(builder.build());
    }
}
