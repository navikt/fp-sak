package no.nav.foreldrepenger.domene.person.verge;

import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterer;
import no.nav.foreldrepenger.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.foreldrepenger.behandlingslager.aktør.Aktør;
import no.nav.foreldrepenger.behandlingslager.aktør.NavBruker;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkAktør;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.Historikkinnslag2;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.Historikkinnslag2Repository;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagLinjeBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.skjermlenke.SkjermlenkeType;
import no.nav.foreldrepenger.behandlingslager.behandling.verge.VergeEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.verge.VergeOrganisasjonEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.verge.VergeRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.verge.VergeType;
import no.nav.foreldrepenger.domene.bruker.NavBrukerTjeneste;
import no.nav.foreldrepenger.domene.person.PersoninfoAdapter;
import no.nav.foreldrepenger.domene.person.verge.dto.AvklarVergeDto;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.PersonIdent;
import no.nav.vedtak.exception.IntegrasjonException;

@ApplicationScoped
@DtoTilServiceAdapter(dto = AvklarVergeDto.class, adapter = AksjonspunktOppdaterer.class)
public class VergeOppdaterer implements AksjonspunktOppdaterer<AvklarVergeDto> {

    private Historikkinnslag2Repository historikkinnslagRepository;
    private VergeRepository vergeRepository;
    private PersoninfoAdapter personinfoAdapter;
    private NavBrukerTjeneste brukerTjeneste;

    protected VergeOppdaterer() {
        // CDI
    }

    @Inject
    public VergeOppdaterer(Historikkinnslag2Repository historikkinnslagRepository,
                           PersoninfoAdapter personinfoAdapter,
                           VergeRepository vergeRepository,
                           NavBrukerTjeneste brukerTjeneste) {
        this.historikkinnslagRepository = historikkinnslagRepository;
        this.vergeRepository = vergeRepository;
        this.personinfoAdapter = personinfoAdapter;
        this.brukerTjeneste = brukerTjeneste;
    }

    @Override
    public OppdateringResultat oppdater(AvklarVergeDto dto, AksjonspunktOppdaterParameter param) {

        var behandlingId = param.getBehandlingId();
        var vergeBuilder = new VergeEntitet.Builder().gyldigPeriode(dto.getGyldigFom(), dto.getGyldigTom()).medVergeType(dto.getVergeType());
        // Verge må enten være oppgitt med fnr (hent ut fra PDL), eller orgnr
        var fnr = VergeType.ADVOKAT.equals(dto.getVergeType()) || dto.getFnr() == null ? null : new PersonIdent(dto.getFnr());
        if (fnr != null) {
            var vergeAktørId = personinfoAdapter.hentAktørForFnr(fnr).orElseThrow(() -> new IllegalArgumentException("Ugyldig FNR for Verge"));
            vergeBuilder.medBruker(hentEllerOpprettBruker(vergeAktørId));
        } else if (dto.getOrganisasjonsnummer() != null) {
            vergeBuilder.medVergeOrganisasjon(opprettVergeOrganisasjon(dto));
        } else {
            throw new IntegrasjonException("FP-905999", "Verge med fnr ikke funnet i PDL, og organisasjonsnummer er heller ikke oppgitt.");
        }
        lagreHistorikkinnslag(dto, param);

        vergeRepository.lagreOgFlush(behandlingId, vergeBuilder);


        return OppdateringResultat.utenOverhopp();
    }

    private NavBruker hentEllerOpprettBruker(AktørId aktoerId) {
        return brukerTjeneste.hentEllerOpprettFraAktørId(aktoerId);
    }

    private VergeOrganisasjonEntitet opprettVergeOrganisasjon(AvklarVergeDto adapter) {
        return new VergeOrganisasjonEntitet.Builder().medOrganisasjonsnummer(adapter.getOrganisasjonsnummer()).medNavn(adapter.getNavn()).build();
    }

    private void lagreHistorikkinnslag(AvklarVergeDto dto, AksjonspunktOppdaterParameter parameter) {
        var historikkBuilder = opprettHistorikkinnslag(dto, parameter);
        historikkinnslagRepository.lagre(historikkBuilder.build());
    }

    private Historikkinnslag2.Builder opprettHistorikkinnslag(AvklarVergeDto dto, AksjonspunktOppdaterParameter parameter) {
        var behandlingId = parameter.getBehandlingId();
        var vergeAggregatOpt = vergeRepository.hentAggregat(behandlingId);

        var historikkBuilder = new Historikkinnslag2.Builder().medFagsakId(parameter.getRef().fagsakId())
            .medBehandlingId(parameter.getRef().behandlingId())
            .medAktør(HistorikkAktør.SAKSBEHANDLER)
            .medTittel(SkjermlenkeType.FAKTA_OM_VERGE);

        if (vergeAggregatOpt.isEmpty() || vergeAggregatOpt.get().getVerge().isEmpty()) {
            historikkBuilder.addLinje("Registrering av opplysninger om verge/fullmektig.");
        } else {
            vergeAggregatOpt.get().getVerge().ifPresent(vergeEntitet -> {
                var aktørId = Optional.ofNullable(vergeEntitet.getBruker()).map(Aktør::getAktørId);
                aktørId.flatMap(id -> personinfoAdapter.hentBrukerArbeidsgiverForAktør(id)).ifPresent(pib -> {
                    historikkBuilder.addlinje(HistorikkinnslagLinjeBuilder.fraTilEquals("Navn", pib.getNavn(), dto.getNavn()));
                    historikkBuilder.addlinje(
                        HistorikkinnslagLinjeBuilder.fraTilEquals("Fødselsnummer", pib.getPersonIdent().getIdent(), dto.getFnr()));
                });
                historikkBuilder.addlinje(
                    HistorikkinnslagLinjeBuilder.fraTilEquals("Periode f.o.m.", vergeEntitet.getGyldigFom(), dto.getGyldigFom()));
                historikkBuilder.addlinje(
                    HistorikkinnslagLinjeBuilder.fraTilEquals("Periode t.o.m.", vergeEntitet.getGyldigTom(), dto.getGyldigTom()));
                historikkBuilder.addlinje(
                    HistorikkinnslagLinjeBuilder.fraTilEquals("Type verge", vergeEntitet.getVergeType(), dto.getVergeType()));
                if (vergeEntitet.getVergeOrganisasjon().isPresent()) {
                    var vergeOrg = vergeEntitet.getVergeOrganisasjon().get();
                    historikkBuilder.addlinje(HistorikkinnslagLinjeBuilder.fraTilEquals("Navn", vergeOrg.getNavn(), dto.getNavn()));
                    historikkBuilder.addlinje(
                        HistorikkinnslagLinjeBuilder.fraTilEquals("Organisasjonsnummer", vergeOrg.getOrganisasjonsnummer(),
                            dto.getOrganisasjonsnummer()));
                }
            });
        }
        historikkBuilder.addLinje(dto.getBegrunnelse());
        return historikkBuilder;
    }

}
