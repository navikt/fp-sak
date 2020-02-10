package no.nav.foreldrepenger.domene.person.verge;

import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterer;
import no.nav.foreldrepenger.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.foreldrepenger.behandlingslager.aktør.Personinfo;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkAktør;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkEndretFeltType;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.Historikkinnslag;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagType;
import no.nav.foreldrepenger.behandlingslager.behandling.skjermlenke.SkjermlenkeType;
import no.nav.foreldrepenger.behandlingslager.behandling.verge.VergeAggregat;
import no.nav.foreldrepenger.behandlingslager.behandling.verge.VergeEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.verge.VergeRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.verge.VergeType;
import no.nav.foreldrepenger.domene.person.tps.TpsTjeneste;
import no.nav.foreldrepenger.domene.person.verge.dto.AvklarVergeDto;
import no.nav.foreldrepenger.domene.person.verge.dto.VergeAksjonpunktDto;
import no.nav.foreldrepenger.domene.personopplysning.PersonopplysningTjeneste;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.PersonIdent;
import no.nav.foreldrepenger.historikk.HistorikkInnslagTekstBuilder;
import no.nav.foreldrepenger.historikk.HistorikkTjenesteAdapter;

@ApplicationScoped
@DtoTilServiceAdapter(dto = AvklarVergeDto.class, adapter = AksjonspunktOppdaterer.class)
public class VergeOppdaterer implements AksjonspunktOppdaterer<AvklarVergeDto> {

    private PersonopplysningTjeneste personopplysningTjeneste;
    private HistorikkTjenesteAdapter historikkAdapter;
    private VergeRepository vergeRepository;
    private TpsTjeneste tpsTjeneste;

    protected VergeOppdaterer() {
        // CDI
    }

    @Inject
    public VergeOppdaterer(PersonopplysningTjeneste personopplysningTjeneste,
                           HistorikkTjenesteAdapter historikkAdapter,
                           TpsTjeneste tpsTjeneste,
                           VergeRepository vergeRepository) {
        this.personopplysningTjeneste = personopplysningTjeneste;
        this.historikkAdapter = historikkAdapter;
        this.vergeRepository = vergeRepository;
        this.tpsTjeneste = tpsTjeneste;
    }

    @Override
    public OppdateringResultat oppdater(AvklarVergeDto dto, AksjonspunktOppdaterParameter param) {
        PersonIdent fnr = null;
        if (vergeErBasertPåFnr(dto)) {
            fnr = dto.getFnr() == null ? null : new PersonIdent(dto.getFnr());
        }
        final VergeAksjonpunktDto adapter = new VergeAksjonpunktDto(fnr, dto.getGyldigFom(), dto.getGyldigTom(),
            dto.getVergeType().getKode(), dto.getNavn(), dto.getOrganisasjonsnummer());

        Long behandlingId = param.getBehandlingId();
        byggHistorikkinnslag(dto, param);

        personopplysningTjeneste.aksjonspunktVergeOppdaterer(behandlingId, adapter);
        return OppdateringResultat.utenOveropp();
    }

    private boolean vergeErBasertPåFnr(AvklarVergeDto dto) {
        return !VergeType.ADVOKAT.equals(dto.getVergeType());
    }

    private void byggHistorikkinnslag(AvklarVergeDto dto, AksjonspunktOppdaterParameter parameter) {
        Long behandlingId = parameter.getBehandlingId();
        Optional<VergeAggregat> vergeAggregatOpt = vergeRepository.hentAggregat(behandlingId);
        if (!vergeAggregatOpt.isPresent() || !vergeAggregatOpt.get().getVerge().isPresent()) {
            HistorikkInnslagTekstBuilder tekstBuilder = new HistorikkInnslagTekstBuilder()
                .medSkjermlenke(SkjermlenkeType.FAKTA_OM_VERGE);
            lagHistorikkinnslag(behandlingId, tekstBuilder, HistorikkinnslagType.REGISTRER_OM_VERGE);
        } else {
            opprettHistorikkinnslagForEndring(dto, parameter, vergeAggregatOpt.get());
        }
    }

    private void opprettHistorikkinnslagForEndring(AvklarVergeDto dto, AksjonspunktOppdaterParameter parameter, VergeAggregat vergeAggregat) {
        HistorikkInnslagTekstBuilder tekstBuilder = new HistorikkInnslagTekstBuilder();
        Optional<AktørId> aktørId = vergeAggregat.getAktørId();
        if (aktørId.isPresent()) {
            Optional<Personinfo> personinfo = tpsTjeneste.hentBrukerForAktør(aktørId.get());
            if (personinfo.isPresent()) {
                tekstBuilder.medEndretFelt(HistorikkEndretFeltType.NAVN, personinfo.get().getNavn(), dto.getNavn());
                tekstBuilder.medEndretFelt(HistorikkEndretFeltType.FNR, personinfo.get().getPersonIdent().getIdent(), dto.getFnr());
            }
        }
        if (vergeAggregat.getVerge().isPresent()) {
            VergeEntitet vergeEntitet = vergeAggregat.getVerge().get();
            tekstBuilder.medEndretFelt(HistorikkEndretFeltType.PERIODE_FOM, vergeEntitet.getGyldigFom(), dto.getGyldigFom());
            tekstBuilder.medEndretFelt(HistorikkEndretFeltType.PERIODE_TOM, vergeEntitet.getGyldigTom(), dto.getGyldigTom());
            tekstBuilder.medEndretFelt(HistorikkEndretFeltType.TYPE_VERGE, vergeEntitet.getVergeType(), dto.getVergeType());
            if (vergeEntitet.getVergeOrganisasjon().isPresent()) {
                var vergeOrg = vergeEntitet.getVergeOrganisasjon().get();
                tekstBuilder.medEndretFelt(HistorikkEndretFeltType.NAVN, vergeOrg.getNavn(), dto.getNavn());
                tekstBuilder.medEndretFelt(HistorikkEndretFeltType.ORGANISASJONSNUMMER, vergeOrg.getOrganisasjonsnummer(), dto.getOrganisasjonsnummer());
            }
        }
        tekstBuilder
            .medBegrunnelse(dto.getBegrunnelse(), parameter.erBegrunnelseEndret())
            .medSkjermlenke(SkjermlenkeType.FAKTA_OM_VERGE);
        lagHistorikkinnslag(parameter.getBehandlingId(), tekstBuilder, HistorikkinnslagType.FAKTA_ENDRET);
    }

    private void lagHistorikkinnslag(Long behandlingId, HistorikkInnslagTekstBuilder tekstBuilder, HistorikkinnslagType innslagType) {
        Historikkinnslag innslag = new Historikkinnslag();

        innslag.setAktør(HistorikkAktør.SAKSBEHANDLER);
        innslag.setBehandlingId(behandlingId);
        innslag.setType(innslagType);
        if (HistorikkinnslagType.REGISTRER_OM_VERGE.equals(innslagType)) {
            tekstBuilder.medHendelse(innslagType);
        }
        tekstBuilder.build(innslag);
        historikkAdapter.lagInnslag(innslag);
    }
}
