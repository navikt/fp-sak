package no.nav.foreldrepenger.familiehendelse.aksjonspunkt;

import java.time.LocalDate;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterer;
import no.nav.foreldrepenger.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.AdopsjonEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.OmsorgsovertakelseVilkårType;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkEndretFeltType;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkEndretFeltVerdiType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.skjermlenke.SkjermlenkeType;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.Vilkår;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.familiehendelse.FamilieHendelseTjeneste;
import no.nav.foreldrepenger.familiehendelse.aksjonspunkt.dto.AvklarFaktaForOmsorgOgForeldreansvarAksjonspunktDto;
import no.nav.foreldrepenger.historikk.HistorikkTjenesteAdapter;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktRegisterinnhentingTjeneste;
import no.nav.vedtak.exception.FunksjonellException;

@ApplicationScoped
@DtoTilServiceAdapter(dto = AvklarFaktaForOmsorgOgForeldreansvarAksjonspunktDto.class, adapter = AksjonspunktOppdaterer.class)
public class AvklarOmsorgOgForeldreansvarOppdaterer implements AksjonspunktOppdaterer<AvklarFaktaForOmsorgOgForeldreansvarAksjonspunktDto> {

    private static final Map<VilkårType, OmsorgsovertakelseVilkårType> MAP_VILKÅR_OMSORGSVILKÅR = Map.of(
        VilkårType.OMSORGSVILKÅRET, OmsorgsovertakelseVilkårType.OMSORGSVILKÅRET,
        VilkårType.FORELDREANSVARSVILKÅRET_2_LEDD, OmsorgsovertakelseVilkårType.FORELDREANSVARSVILKÅRET_2_LEDD,
        VilkårType.FORELDREANSVARSVILKÅRET_4_LEDD, OmsorgsovertakelseVilkårType.FORELDREANSVARSVILKÅRET_4_LEDD
    );

    private SkjæringstidspunktRegisterinnhentingTjeneste skjæringstidspunktTjeneste;
    private FamilieHendelseTjeneste familieHendelseTjeneste;
    private HistorikkTjenesteAdapter historikkAdapter;
    private BehandlingRepositoryProvider repositoryProvider;
    private BehandlingsresultatRepository behandlingsresultatRepository;

    AvklarOmsorgOgForeldreansvarOppdaterer() {
        // for CDI proxy
    }

    @Inject
    public AvklarOmsorgOgForeldreansvarOppdaterer(BehandlingRepositoryProvider repositoryProvider,
                                                  SkjæringstidspunktRegisterinnhentingTjeneste skjæringstidspunktTjeneste,
                                                  FamilieHendelseTjeneste familieHendelseTjeneste,
                                                  HistorikkTjenesteAdapter historikkAdapter) {
        this.repositoryProvider = repositoryProvider;
        this.behandlingsresultatRepository = repositoryProvider.getBehandlingsresultatRepository();
        this.skjæringstidspunktTjeneste = skjæringstidspunktTjeneste;
        this.familieHendelseTjeneste = familieHendelseTjeneste;
        this.historikkAdapter = historikkAdapter;
    }

    @Override
    public boolean skalReinnhenteRegisteropplysninger(Long behandlingId, LocalDate forrigeSkjæringstidspunkt) {
        return !skjæringstidspunktTjeneste.utledSkjæringstidspunktForRegisterInnhenting(behandlingId).equals(forrigeSkjæringstidspunkt);
    }

    @Override
    public OppdateringResultat oppdater(AvklarFaktaForOmsorgOgForeldreansvarAksjonspunktDto dto, AksjonspunktOppdaterParameter param) {
        var behandlingId = param.getBehandlingId();
        var totrinn = håndterEndringHistorikk(dto, param);

        final var forrigeSkjæringstidspunkt = skjæringstidspunktTjeneste.utledSkjæringstidspunktForRegisterInnhenting(behandlingId);

        var builder = OppdateringResultat.utenTransisjon();

        oppdaterAksjonspunktGrunnlag(dto, param);

        var skalReinnhenteRegisteropplysninger = skalReinnhenteRegisteropplysninger(behandlingId, forrigeSkjæringstidspunkt);

        // Aksjonspunkter
        settNyttVilkårOgAvbrytAndreOmsorgsovertakelseVilkårOgAksjonspunkter(dto, param, builder);

        if (skalReinnhenteRegisteropplysninger) {
            return builder.medTotrinnHvis(totrinn).medOppdaterGrunnlag().build();
        }
        return builder.medTotrinnHvis(totrinn).build();
    }

    private void oppdaterAksjonspunktGrunnlag(AvklarFaktaForOmsorgOgForeldreansvarAksjonspunktDto dto, AksjonspunktOppdaterParameter param) {
        var behandling = param.getBehandling();

        var omsorgsovertakelseVilkårType = Optional.ofNullable(MAP_VILKÅR_OMSORGSVILKÅR.get(dto.getVilkårType()))
            .orElseThrow(() -> new FunksjonellException("FP-765341", "Mangler vilkårtype", "Oppgi vilkårtype"));
        final var oppdatertOverstyrtHendelse = familieHendelseTjeneste.opprettBuilderFor(behandling);
        oppdatertOverstyrtHendelse
            .medAdopsjon(oppdatertOverstyrtHendelse.getAdopsjonBuilder()
                .medOmsorgovertalseVilkårType(omsorgsovertakelseVilkårType)
                .medOmsorgsovertakelseDato(dto.getOmsorgsovertakelseDato()));
        familieHendelseTjeneste.lagreOverstyrtHendelse(behandling, oppdatertOverstyrtHendelse);
    }

    private void settNyttVilkårOgAvbrytAndreOmsorgsovertakelseVilkårOgAksjonspunkter(AvklarFaktaForOmsorgOgForeldreansvarAksjonspunktDto dto,
                                                                                     AksjonspunktOppdaterParameter param,
                                                                                     OppdateringResultat.Builder builder) {

        // Omsorgsovertakelse
        var vilkårType = dto.getVilkårType();
        if (vilkårType == null || VilkårType.UDEFINERT.equals(vilkårType)) {
            throw new FunksjonellException("FP-381974", "Vilkår ikke valgt", "Velg hvilket vilkår som kommer til anvendelse");
        }

        builder.leggTilIkkeVurdertVilkår(vilkårType);

        var behandling = param.getBehandling();
        // Rydd opp i eventuelle omsorgsvilkår som er tidligere lagt til
        var behandlingResultat = getBehandlingsresultat(param.getBehandlingId());

        if (behandlingResultat != null) {
            behandlingResultat.getVilkårResultat().getVilkårene().stream()
                .filter(vilkår -> OmsorgsvilkårKonfigurasjon.OMSORGS_VILKÅR.contains(vilkår.getVilkårType()))
                // Men uten å fjerne seg selv
                .filter(vilkår -> !vilkår.getVilkårType().equals(vilkårType))
                .forEach(fjernet -> builder.fjernVilkårType(fjernet.getVilkårType()));
        }
        behandling.getAksjonspunkter().stream()
            .filter(ap -> OmsorgsvilkårKonfigurasjon.OMSORGS_AKSJONSPUNKT.contains(ap.getAksjonspunktDefinisjon()))
            .forEach(ap -> builder.medEkstraAksjonspunktResultat(ap.getAksjonspunktDefinisjon(), AksjonspunktStatus.AVBRUTT));
    }

    private boolean håndterEndringHistorikk(AvklarFaktaForOmsorgOgForeldreansvarAksjonspunktDto dto, AksjonspunktOppdaterParameter param) {
        boolean erEndret;

        var behandlingId = param.getBehandlingId();
        var behandling = param.getBehandling();
        final var hendelseGrunnlag = repositoryProvider.getFamilieHendelseRepository().hentAggregat(behandlingId);

        var orginalOmsorgsovertakelseDato = getOriginalOmsorgsovertakelseDato(hendelseGrunnlag);
        erEndret = oppdaterVedEndretVerdi(HistorikkEndretFeltType.OMSORGSOVERTAKELSESDATO,
            orginalOmsorgsovertakelseDato.orElse(null), dto.getOmsorgsovertakelseDato());

        var vilkårType = dto.getVilkårType();
        var vilkårTyper = getBehandlingsresultat(behandlingId).getVilkårResultat().getVilkårene().stream()
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
        }
        if (VilkårType.FORELDREANSVARSVILKÅRET_2_LEDD.equals(vilkårType)) {
            return HistorikkEndretFeltVerdiType.FORELDREANSVAR_2_TITTEL;
        }
        if (VilkårType.FORELDREANSVARSVILKÅRET_4_LEDD.equals(vilkårType)) {
            return HistorikkEndretFeltVerdiType.FORELDREANSVAR_4_TITTEL;
        }
        return null;
    }

    private Optional<LocalDate> getOriginalOmsorgsovertakelseDato(FamilieHendelseGrunnlagEntitet grunnlag) {
        return grunnlag.getGjeldendeAdopsjon().map(AdopsjonEntitet::getOmsorgsovertakelseDato);
    }

    private boolean oppdaterVedEndretVerdi(HistorikkEndretFeltType type, LocalDate original, LocalDate bekreftet) {
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
