package no.nav.foreldrepenger.familiehendelse.aksjonspunkt;

import static java.util.stream.Collectors.toList;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterer;
import no.nav.foreldrepenger.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.AdopsjonEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.OmsorgsovertakelseVilkårType;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.UidentifisertBarn;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.UidentifisertBarnEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkEndretFeltType;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkEndretFeltVerdiType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.skjermlenke.SkjermlenkeType;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.Vilkår;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårResultat.Builder;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårUtfallType;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.domene.personopplysning.AvklarOmsorgOgForeldreansvarAksjonspunktData;
import no.nav.foreldrepenger.domene.personopplysning.AvklartDataBarnAdapter;
import no.nav.foreldrepenger.familiehendelse.aksjonspunkt.dto.AvklarFaktaForOmsorgOgForeldreansvarAksjonspunktDto;
import no.nav.foreldrepenger.familiehendelse.omsorg.OmsorghendelseTjeneste;
import no.nav.foreldrepenger.familiehendelse.omsorg.OmsorgsvilkårKonfigurasjon;
import no.nav.foreldrepenger.familiehendelse.rest.AvklartDataBarnDto;
import no.nav.foreldrepenger.historikk.HistorikkTjenesteAdapter;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktRegisterinnhentingTjeneste;

@ApplicationScoped
@DtoTilServiceAdapter(dto = AvklarFaktaForOmsorgOgForeldreansvarAksjonspunktDto.class, adapter = AksjonspunktOppdaterer.class)
public class AvklarOmsorgOgForeldreansvarOppdaterer implements AksjonspunktOppdaterer<AvklarFaktaForOmsorgOgForeldreansvarAksjonspunktDto> {

    private SkjæringstidspunktRegisterinnhentingTjeneste skjæringstidspunktTjeneste;
    private OmsorghendelseTjeneste omsorghendelseTjeneste;
    private HistorikkTjenesteAdapter historikkAdapter;
    private BehandlingRepositoryProvider repositoryProvider;
    private BehandlingsresultatRepository behandlingsresultatRepository;

    AvklarOmsorgOgForeldreansvarOppdaterer() {
        // for CDI proxy
    }

    @Inject
    public AvklarOmsorgOgForeldreansvarOppdaterer(BehandlingRepositoryProvider repositoryProvider,
                                                  SkjæringstidspunktRegisterinnhentingTjeneste skjæringstidspunktTjeneste,
                                                  OmsorghendelseTjeneste omsorghendelseTjeneste,
                                                  HistorikkTjenesteAdapter historikkAdapter) {
        this.repositoryProvider = repositoryProvider;
        this.behandlingsresultatRepository = repositoryProvider.getBehandlingsresultatRepository();
        this.skjæringstidspunktTjeneste = skjæringstidspunktTjeneste;
        this.omsorghendelseTjeneste = omsorghendelseTjeneste;
        this.historikkAdapter = historikkAdapter;
    }

    @Override
    public boolean skalReinnhenteRegisteropplysninger(Long behandlingId, LocalDate forrigeSkjæringstidspunkt) {
        return !skjæringstidspunktTjeneste.utledSkjæringstidspunktForRegisterInnhenting(behandlingId).equals(forrigeSkjæringstidspunkt);
    }

    @Override
    public OppdateringResultat oppdater(AvklarFaktaForOmsorgOgForeldreansvarAksjonspunktDto dto, AksjonspunktOppdaterParameter param) {
        Long behandlingId = param.getBehandlingId();
        boolean totrinn = håndterEndringHistorikk(dto, param);

        final LocalDate forrigeSkjæringstidspunkt = skjæringstidspunktTjeneste.utledSkjæringstidspunktForRegisterInnhenting(behandlingId);

        OppdateringResultat.Builder builder = OppdateringResultat.utenTransisjon();

        oppdaterAksjonspunktGrunnlag(dto, param, builder);

        boolean skalReinnhenteRegisteropplysninger = skalReinnhenteRegisteropplysninger(behandlingId, forrigeSkjæringstidspunkt);

        // Aksjonspunkter
        settNyttVilkårOgAvbrytAndreOmsorgsovertakelseVilkårOgAksjonspunkter(dto, param, builder);

        if (skalReinnhenteRegisteropplysninger) {
            return builder.medTotrinnHvis(totrinn).medOppdaterGrunnlag().build();
        } else {
            return builder.medTotrinnHvis(totrinn).build();
        }
    }

    private void oppdaterAksjonspunktGrunnlag(AvklarFaktaForOmsorgOgForeldreansvarAksjonspunktDto dto, AksjonspunktOppdaterParameter param,
                                              OppdateringResultat.Builder builder) {
        Behandling behandling = param.getBehandling();
        List<AvklartDataBarnAdapter> barnAdapter = new ArrayList<>();
        dto.getBarn().forEach(barn -> barnAdapter.add(new AvklartDataBarnAdapter(barn.getAktørId(), barn.getFodselsdato(), barn.getNummer())));

        AksjonspunktDefinisjon aksjonspunktDefinisjon = AksjonspunktDefinisjon.fraKode(dto.getKode());

        final AvklarOmsorgOgForeldreansvarAksjonspunktData data = new AvklarOmsorgOgForeldreansvarAksjonspunktData(dto.getVilkårType().getKode(),
            aksjonspunktDefinisjon, dto.getOmsorgsovertakelseDato(), dto.getAntallBarn(), barnAdapter);

        omsorghendelseTjeneste.aksjonspunktAvklarOmsorgOgForeldreansvar(behandling, data, builder);
    }

    private void settNyttVilkårOgAvbrytAndreOmsorgsovertakelseVilkårOgAksjonspunkter(AvklarFaktaForOmsorgOgForeldreansvarAksjonspunktDto dto,
                                                                                     AksjonspunktOppdaterParameter param,
                                                                                     OppdateringResultat.Builder builder) {

        // Omsorgsovertakelse
        OmsorgsovertakelseVilkårType omsorgsovertakelseVilkårType = OmsorgsovertakelseVilkårType.fraKode(dto.getVilkårType().getKode());
        // Vilkår
        VilkårType vilkårType = VilkårType.fraKode(dto.getVilkårType().getKode());

        builder.leggTilVilkårResultat(vilkårType, VilkårUtfallType.IKKE_VURDERT);

        Behandling behandling = param.getBehandling();
        // Rydd opp i eventuelle omsorgsvilkår som er tidligere lagt til
        var behandlingResultat = getBehandlingsresultat(param.getBehandlingId());

        if (behandlingResultat != null) {
            behandlingResultat.getVilkårResultat().getVilkårene().stream()
                .filter(vilkår -> OmsorgsvilkårKonfigurasjon.getOmsorgsovertakelseVilkår().contains(vilkår.getVilkårType()))
                // Men uten å fjerne seg selv
                .filter(vilkår -> !vilkår.getVilkårType().getKode().equals(omsorgsovertakelseVilkårType.getKode()))
                .forEach(fjernet -> builder.fjernVilkårType(fjernet.getVilkårType()));
        }
        AksjonspunktDefinisjon aksjonspunktDefinisjon = AksjonspunktDefinisjon.fraKode(dto.getKode());
        behandling.getAksjonspunkter().stream()
            .filter(ap -> OmsorgsvilkårKonfigurasjon.getOmsorgsovertakelseAksjonspunkter().contains(ap.getAksjonspunktDefinisjon()))
            .filter(ap -> !Objects.equals(ap.getAksjonspunktDefinisjon(), aksjonspunktDefinisjon)) // ikke avbryte seg selv
            .forEach(ap -> builder.medEkstraAksjonspunktResultat(ap.getAksjonspunktDefinisjon(), AksjonspunktStatus.AVBRUTT));
    }

    private boolean håndterEndringHistorikk(AvklarFaktaForOmsorgOgForeldreansvarAksjonspunktDto dto, AksjonspunktOppdaterParameter param) {
        boolean erEndret;

        Long behandlingId = param.getBehandlingId();
        Behandling behandling = param.getBehandling();
        final FamilieHendelseGrunnlagEntitet hendelseGrunnlag = repositoryProvider.getFamilieHendelseRepository().hentAggregat(behandlingId);

        Optional<LocalDate> orginalOmsorgsovertakelseDato = getOriginalOmsorgsovertakelseDato(hendelseGrunnlag);
        erEndret = oppdaterVedEndretVerdi(HistorikkEndretFeltType.OMSORGSOVERTAKELSESDATO,
            orginalOmsorgsovertakelseDato.orElse(null), dto.getOmsorgsovertakelseDato());

        Integer orginalAntallBarn = getOrginalAntallBarnForOmsorgsovertakelse(hendelseGrunnlag);
        erEndret = oppdaterVedEndretVerdi(HistorikkEndretFeltType.ANTALL_BARN, orginalAntallBarn, dto.getAntallBarn()) || erEndret;

        List<UidentifisertBarn> orginaleBarn = getOpprinneligeBarn(behandlingId);
        List<UidentifisertBarn> oppdaterteBarn = getOppdaterteBarn(dto);
        erEndret = oppdaterVedEndringAvFødselsdatoer(orginaleBarn, oppdaterteBarn) || erEndret;

        VilkårType vilkårType = dto.getVilkårType();
        List<VilkårType> vilkårTyper = getBehandlingsresultat(behandlingId).getVilkårResultat().getVilkårene().stream()
            .map(Vilkår::getVilkårType)
            .collect(Collectors.toList());
        if (!vilkårTyper.contains(vilkårType)) {
            historikkAdapter.tekstBuilder().medEndretFelt(HistorikkEndretFeltType.VILKAR_SOM_ANVENDES, null, finnTekstBasertPåOmsorgsvilkår(vilkårType));
        }

        historikkAdapter.tekstBuilder()
            .medBegrunnelse(dto.getBegrunnelse(), param.erBegrunnelseEndret())
            .medSkjermlenke(getSkjermlenkeType(behandling.getFagsakYtelseType()));

        return erEndret;
    }

    private SkjermlenkeType getSkjermlenkeType(FagsakYtelseType fagsakYtelseType) {
        return FagsakYtelseType.ENGANGSTØNAD.equals(fagsakYtelseType) ? SkjermlenkeType.FAKTA_OM_OMSORG_OG_FORELDREANSVAR : SkjermlenkeType.FAKTA_FOR_OMSORG;
    }

    private HistorikkEndretFeltVerdiType finnTekstBasertPåOmsorgsvilkår(VilkårType vilkårType) {
        if (VilkårType.OMSORGSVILKÅRET.equals(vilkårType)) {
            return HistorikkEndretFeltVerdiType.OMSORGSVILKARET_TITTEL;
        } else if (VilkårType.FORELDREANSVARSVILKÅRET_2_LEDD.equals(vilkårType)) {
            return HistorikkEndretFeltVerdiType.FORELDREANSVAR_2_TITTEL;
        } else if (VilkårType.FORELDREANSVARSVILKÅRET_4_LEDD.equals(vilkårType)) {
            return HistorikkEndretFeltVerdiType.FORELDREANSVAR_4_TITTEL;
        }
        return null;
    }

    private List<UidentifisertBarn> getOpprinneligeBarn(Long behandlingId) {
        List<UidentifisertBarn> oppgitteBarn = repositoryProvider.getFamilieHendelseRepository().hentAggregat(behandlingId).getGjeldendeVersjon()
            .getBarna().stream()
            .map(barn -> new UidentifisertBarnEntitet(barn.getBarnNummer(), barn.getFødselsdato(), null))
            .collect(toList());

        return oppgitteBarn;
    }

    private List<UidentifisertBarn> getOppdaterteBarn(AvklarFaktaForOmsorgOgForeldreansvarAksjonspunktDto dto) {
        List<AvklartDataBarnDto> barna = dto.getBarn();
        if (barna != null) {
            return barna.stream()
                .map(barn -> new UidentifisertBarnEntitet(barn.getNummer(), barn.getFodselsdato(), null))
                .collect(toList());
        } else {
            return Collections.emptyList();
        }
    }

    private Integer getOrginalAntallBarnForOmsorgsovertakelse(FamilieHendelseGrunnlagEntitet grunnlag) {
        return grunnlag.getGjeldendeAntallBarn();
    }

    private Optional<LocalDate> getOriginalOmsorgsovertakelseDato(FamilieHendelseGrunnlagEntitet grunnlag) {
        return grunnlag.getGjeldendeAdopsjon().map(AdopsjonEntitet::getOmsorgsovertakelseDato);
    }

    private boolean oppdaterVedEndringAvFødselsdatoer(List<UidentifisertBarn> orginalBarn, List<UidentifisertBarn> oppdaterteBarn) {
        boolean erEndret = false;

        // Endrede
        for (UidentifisertBarn opprinnelig : orginalBarn) {
            Optional<UidentifisertBarn> endret = oppdaterteBarn.stream()
                .filter(oppdatert -> opprinnelig.getBarnNummer() != null && oppdatert.getBarnNummer() != null) // Kan
                // bare
                // spore
                // endringer
                // på
                // barn
                // med
                // nummer
                .filter(oppdatert -> opprinnelig.getBarnNummer().equals(oppdatert.getBarnNummer()))
                .filter(oppdatert -> !opprinnelig.getFødselsdato().equals(oppdatert.getFødselsdato()))
                .findFirst();

            if (endret.isPresent()) {
                erEndret = oppdaterVedEndretVerdi(HistorikkEndretFeltType.FODSELSDATO, opprinnelig.getFødselsdato(), endret.get().getFødselsdato())
                    || erEndret;
            }
        }
        return erEndret;
    }

    private boolean oppdaterVedEndretVerdi(HistorikkEndretFeltType type, LocalDate original, LocalDate bekreftet) {
        if (!Objects.equals(bekreftet, original)) {
            historikkAdapter.tekstBuilder().medEndretFelt(type, original, bekreftet);
            return true;
        }
        return false;
    }


    private boolean oppdaterVedEndretVerdi(HistorikkEndretFeltType type, Number original, Number bekreftet) {
        if (!Objects.equals(bekreftet, original)) {
            historikkAdapter.tekstBuilder().medEndretFelt(type, original, bekreftet);
            return true;
        }
        return false;
    }

    private Behandlingsresultat getBehandlingsresultat(Long behandlingId) {
        return behandlingsresultatRepository.hentHvisEksisterer(behandlingId).orElse(null);
    }

}
