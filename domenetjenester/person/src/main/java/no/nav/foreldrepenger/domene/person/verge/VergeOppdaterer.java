package no.nav.foreldrepenger.domene.person.verge;

import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterer;
import no.nav.foreldrepenger.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.foreldrepenger.behandlingslager.aktør.NavBruker;
import no.nav.foreldrepenger.behandlingslager.aktør.NavBrukerRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkAktør;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkEndretFeltType;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.Historikkinnslag;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagType;
import no.nav.foreldrepenger.behandlingslager.behandling.skjermlenke.SkjermlenkeType;
import no.nav.foreldrepenger.behandlingslager.behandling.verge.VergeAggregat;
import no.nav.foreldrepenger.behandlingslager.behandling.verge.VergeBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.verge.VergeEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.verge.VergeOrganisasjonBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.verge.VergeOrganisasjonEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.verge.VergeRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.verge.VergeType;
import no.nav.foreldrepenger.domene.person.PersoninfoAdapter;
import no.nav.foreldrepenger.domene.person.verge.dto.AvklarVergeDto;
import no.nav.foreldrepenger.domene.personopplysning.OppdatererAksjonspunktFeil;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.PersonIdent;
import no.nav.foreldrepenger.historikk.HistorikkInnslagTekstBuilder;
import no.nav.foreldrepenger.historikk.HistorikkTjenesteAdapter;

@ApplicationScoped
@DtoTilServiceAdapter(dto = AvklarVergeDto.class, adapter = AksjonspunktOppdaterer.class)
public class VergeOppdaterer implements AksjonspunktOppdaterer<AvklarVergeDto> {

    private HistorikkTjenesteAdapter historikkAdapter;
    private VergeRepository vergeRepository;
    private PersoninfoAdapter personinfoAdapter;
    private NavBrukerRepository navBrukerRepository;

    protected VergeOppdaterer() {
        // CDI
    }

    @Inject
    public VergeOppdaterer(HistorikkTjenesteAdapter historikkAdapter,
                           PersoninfoAdapter personinfoAdapter,
                           VergeRepository vergeRepository,
                           NavBrukerRepository navBrukerRepository) {
        this.historikkAdapter = historikkAdapter;
        this.vergeRepository = vergeRepository;
        this.personinfoAdapter = personinfoAdapter;
        this.navBrukerRepository = navBrukerRepository;
    }

    @Override
    public OppdateringResultat oppdater(AvklarVergeDto dto, AksjonspunktOppdaterParameter param) {

        Long behandlingId = param.getBehandlingId();
        VergeBuilder vergeBuilder = new VergeBuilder()
            .gyldigPeriode(dto.getGyldigFom(), dto.getGyldigTom())
            .medVergeType(dto.getVergeType());
        // Verge må enten være oppgitt med fnr (hent ut fra TPS), eller orgnr
        PersonIdent fnr = VergeType.ADVOKAT.equals(dto.getVergeType()) || dto.getFnr() == null ? null : new PersonIdent(dto.getFnr());
        if (fnr != null) {
            var vergeAktørId = personinfoAdapter.hentAktørForFnr(fnr).orElseThrow(() -> new IllegalArgumentException("Ugyldig FNR for Verge"));
            vergeBuilder.medBruker(hentEllerOpprettBruker(vergeAktørId));
        } else if (dto.getOrganisasjonsnummer() != null) {
            vergeBuilder.medVergeOrganisasjon(opprettVergeOrganisasjon(dto));
        } else {
            throw OppdatererAksjonspunktFeil.FACTORY.vergeIkkeFunnetITPS().toException();
        }

        vergeRepository.lagreOgFlush(behandlingId, vergeBuilder);

        byggHistorikkinnslag(dto, param);

        return OppdateringResultat.utenOveropp();
    }

    private NavBruker hentEllerOpprettBruker(AktørId aktoerId) {
        return navBrukerRepository.hent(aktoerId)
            .orElseGet(() -> NavBruker.opprettNy(aktoerId, personinfoAdapter.hentForetrukketSpråk(aktoerId).getForetrukketSpråk()));
    }

    private VergeOrganisasjonEntitet opprettVergeOrganisasjon(AvklarVergeDto adapter) {
        return new VergeOrganisasjonBuilder()
            .medOrganisasjonsnummer(adapter.getOrganisasjonsnummer())
            .medNavn(adapter.getNavn())
            .build();
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
        aktørId.flatMap(id -> personinfoAdapter.hentBrukerArbeidsgiverForAktør(id)).ifPresent(pib -> {
            tekstBuilder.medEndretFelt(HistorikkEndretFeltType.NAVN, pib.getNavn(), dto.getNavn());
            tekstBuilder.medEndretFelt(HistorikkEndretFeltType.FNR, pib.getPersonIdent().getIdent(), dto.getFnr());
        });
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
