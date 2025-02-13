package no.nav.foreldrepenger.domene.person.verge;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.foreldrepenger.behandlingslager.aktør.Aktør;
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

import static no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagLinjeBuilder.fraTilEquals;

@ApplicationScoped
public class OpprettVergeTjeneste {

    private PersoninfoAdapter personinfoAdapter;
    private NavBrukerTjeneste brukerTjeneste;
    private VergeRepository vergeRepository;
    private HistorikkinnslagRepository historikkinnslagRepository;

    @Inject
    public OpprettVergeTjeneste(PersoninfoAdapter personinfoAdapter, NavBrukerTjeneste brukerTjeneste, VergeRepository vergeRepository, HistorikkinnslagRepository historikkinnslagRepository) {
        this.personinfoAdapter = personinfoAdapter;
        this.brukerTjeneste = brukerTjeneste;
        this.vergeRepository = vergeRepository;
        this.historikkinnslagRepository = historikkinnslagRepository;
    }

    OpprettVergeTjeneste() {
        // CDI
    }

    public void opprettVerge(Long behandlingId, Long fagsakId, OpprettVergeDto dto) {
        var vergeBuilder = new VergeEntitet.Builder()
                .gyldigPeriode(dto.gyldigFom(), dto.gyldigTom())
                .medVergeType(dto.vergeType());

        if (VergeType.ADVOKAT.equals(dto.vergeType())) {
            vergeBuilder.medVergeOrganisasjon(opprettVergeOrganisasjon(dto));
        } else {
            var personIdent = new PersonIdent(dto.fnr());
            vergeBuilder.medBruker(hentEllerOpprettBruker(personIdent));
        }

        lagreHistorikkinnslag(behandlingId, fagsakId, dto);

        vergeRepository.lagreOgFlush(behandlingId, vergeBuilder);
    }

    private NavBruker hentEllerOpprettBruker(PersonIdent personIdent) {

        return personinfoAdapter.hentAktørForFnr(personIdent)
                .map(vergeAktorId -> brukerTjeneste.hentEllerOpprettFraAktørId(vergeAktorId))
                .orElseThrow(() -> new IllegalArgumentException("Ugyldig FNR for Verge"));
    }

    private VergeOrganisasjonEntitet opprettVergeOrganisasjon(OpprettVergeDto dto) {
        return new VergeOrganisasjonEntitet.Builder().medOrganisasjonsnummer(dto.organisasjonsnummer()).medNavn(dto.navn()).build();
    }

    private void lagreHistorikkinnslag(Long behandlingId, Long fagsakId, OpprettVergeDto dto) {
        var historikkBuilder = opprettHistorikkinnslag(behandlingId, fagsakId, dto);
        historikkinnslagRepository.lagre(historikkBuilder.build());
    }

    private Historikkinnslag.Builder opprettHistorikkinnslag(Long behandlingId, Long fagsakId, OpprettVergeDto dto) {
        var vergeAggregatOpt = vergeRepository.hentAggregat(behandlingId);

        var builder = new Historikkinnslag.Builder().medFagsakId(fagsakId)
                .medBehandlingId(behandlingId)
                .medAktør(HistorikkAktør.SAKSBEHANDLER)
                .medTittel(SkjermlenkeType.FAKTA_OM_VERGE);


        var vaOpt = vergeAggregatOpt.flatMap(VergeAggregat::getVerge);

        vaOpt.ifPresentOrElse(va -> {
            var personInfoVerge = va.getBruker()
                    .map(Aktør::getAktørId)
                    .flatMap(id -> personinfoAdapter.hentBrukerArbeidsgiverForAktør(id));

            personInfoVerge.ifPresent(pib -> {
                builder.addLinje(fraTilEquals("Navn", pib.getNavn(), dto.navn()));
                builder.addLinje(fraTilEquals("Fødselsnummer", pib.getPersonIdent().getIdent(), dto.fnr()));
            });
            va.getVergeOrganisasjon().ifPresent(vergeOrg -> {
                builder.addLinje(fraTilEquals("Navn", vergeOrg.getNavn(), dto.navn()));
                builder.addLinje(
                        fraTilEquals("Organisasjonsnummer", vergeOrg.getOrganisasjonsnummer(),
                                dto.organisasjonsnummer()));
            });
            builder.addLinje(
                    fraTilEquals("Type verge", va.getVergeType(), dto.vergeType()));

            builder.addLinje(
                    fraTilEquals("Periode f.o.m.", va.getGyldigFom(), dto.gyldigFom()));
            builder.addLinje(
                    fraTilEquals("Periode t.o.m.", va.getGyldigTom(), dto.gyldigTom()));

        }, () ->
                builder.addLinje("Registrering av opplysninger om verge/fullmektig."));

        builder.addLinje(dto.begrunnelse());

        return builder;
    }
}
