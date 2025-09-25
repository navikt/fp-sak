package no.nav.foreldrepenger.familiehendelse.aksjonspunkt.omsorgsovertagelse;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterer;
import no.nav.foreldrepenger.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.AdopsjonEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.OmsorgsovertakelseVilkårType;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkAktør;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.Historikkinnslag;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagLinjeBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.skjermlenke.SkjermlenkeType;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.Vilkår;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.familiehendelse.FamilieHendelseTjeneste;
import no.nav.foreldrepenger.familiehendelse.aksjonspunkt.OmsorgsvilkårKonfigurasjon;
import no.nav.foreldrepenger.familiehendelse.aksjonspunkt.omsorgsovertagelse.dto.AvklarFaktaForOmsorgOgForeldreansvarAksjonspunktDto;
import no.nav.foreldrepenger.skjæringstidspunkt.OpplysningsPeriodeTjeneste;
import no.nav.vedtak.exception.FunksjonellException;

@ApplicationScoped
@DtoTilServiceAdapter(dto = AvklarFaktaForOmsorgOgForeldreansvarAksjonspunktDto.class, adapter = AksjonspunktOppdaterer.class)
public class AvklarOmsorgOgForeldreansvarOppdaterer implements AksjonspunktOppdaterer<AvklarFaktaForOmsorgOgForeldreansvarAksjonspunktDto> {

    private static final Map<VilkårType, OmsorgsovertakelseVilkårType> MAP_VILKÅR_OMSORGSVILKÅR = Map.of(VilkårType.OMSORGSVILKÅRET,
        OmsorgsovertakelseVilkårType.OMSORGSVILKÅRET, VilkårType.FORELDREANSVARSVILKÅRET_2_LEDD,
        OmsorgsovertakelseVilkårType.FORELDREANSVARSVILKÅRET_2_LEDD, VilkårType.FORELDREANSVARSVILKÅRET_4_LEDD,
        OmsorgsovertakelseVilkårType.FORELDREANSVARSVILKÅRET_4_LEDD);

    private FamilieHendelseTjeneste familieHendelseTjeneste;
    private BehandlingsresultatRepository behandlingsresultatRepository;
    private BehandlingRepository behandlingRepository;
    private OpplysningsPeriodeTjeneste opplysningsPeriodeTjeneste;
    private HistorikkinnslagRepository historikkinnslagRepository;

    AvklarOmsorgOgForeldreansvarOppdaterer() {
        // for CDI proxy
    }

    @Inject
    public AvklarOmsorgOgForeldreansvarOppdaterer(BehandlingRepositoryProvider repositoryProvider,
                                                  OpplysningsPeriodeTjeneste opplysningsPeriodeTjeneste,
                                                  FamilieHendelseTjeneste familieHendelseTjeneste) {
        this.behandlingsresultatRepository = repositoryProvider.getBehandlingsresultatRepository();
        this.familieHendelseTjeneste = familieHendelseTjeneste;
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.opplysningsPeriodeTjeneste = opplysningsPeriodeTjeneste;
        this.historikkinnslagRepository = repositoryProvider.getHistorikkinnslagRepository();
    }

    @Override
    public OppdateringResultat oppdater(AvklarFaktaForOmsorgOgForeldreansvarAksjonspunktDto dto, AksjonspunktOppdaterParameter param) {
        var behandlingId = param.getBehandlingId();
        var forrigeFikspunkt = opplysningsPeriodeTjeneste.utledFikspunktForRegisterInnhenting(behandlingId, param.getRef().fagsakYtelseType());

        var hendelseGrunnlag = familieHendelseTjeneste.hentAggregat(behandlingId);
        var originalOmsorgsovertakelseDato = getOriginalOmsorgsovertakelseDato(hendelseGrunnlag).orElse(null);

        var vilkårType = dto.getVilkårType();
        var vilkårTyper = behandlingsresultatRepository.hent(behandlingId)
            .getVilkårResultat()
            .getVilkårene()
            .stream()
            .map(Vilkår::getVilkårType)
            .toList();

        var erEndret = lagreHistorikk(dto, param, vilkårTyper, originalOmsorgsovertakelseDato, vilkårType);

        var builder = OppdateringResultat.utenTransisjon();

        oppdaterAksjonspunktGrunnlag(dto, behandlingId);

        // Aksjonspunkter
        settNyttVilkårOgAvbrytAndreOmsorgsovertakelseVilkårOgAksjonspunkter(dto, builder, behandlingId);

        var sistefikspunkt = opplysningsPeriodeTjeneste.utledFikspunktForRegisterInnhenting(behandlingId, param.getRef().fagsakYtelseType());
        if (!Objects.equals(forrigeFikspunkt, sistefikspunkt)) {
            builder.medOppdaterGrunnlag();
        }
        return builder.medTotrinnHvis(erEndret).build();
    }

    private boolean lagreHistorikk(AvklarFaktaForOmsorgOgForeldreansvarAksjonspunktDto dto,
                               AksjonspunktOppdaterParameter param,
                               List<VilkårType> vilkårTyper,
                               LocalDate originalOmsorgsovertakelseDato,
                               VilkårType vilkårType) {
        var nyttVilkårType = !vilkårTyper.contains(dto.getVilkårType());
        var erEndret =
            nyttVilkårType || param.erBegrunnelseEndret() || !Objects.equals(originalOmsorgsovertakelseDato, dto.getOmsorgsovertakelseDato());
        if (erEndret) {
            var historikkBuilder = new Historikkinnslag.Builder().medAktør(HistorikkAktør.SAKSBEHANDLER)
                .medTittel(getSkjermlenkeType(param.getRef().fagsakYtelseType()))
                .medBehandlingId(param.getBehandlingId())
                .medFagsakId(param.getFagsakId())
                .addLinje(HistorikkinnslagLinjeBuilder.fraTilEquals("Omsorgsovertakelsesdato", originalOmsorgsovertakelseDato,
                    dto.getOmsorgsovertakelseDato()));

            if (nyttVilkårType) {
                historikkBuilder.addLinje(new HistorikkinnslagLinjeBuilder().til("Vilkår som anvendes", finnTekstBasertPåOmsorgsvilkår(
                    vilkårType)));
            }
            historikkinnslagRepository.lagre(historikkBuilder.addLinje(dto.getBegrunnelse()).build());
        }
        return erEndret;
    }

    private void oppdaterAksjonspunktGrunnlag(AvklarFaktaForOmsorgOgForeldreansvarAksjonspunktDto dto, Long behandlingId) {
        var omsorgsovertakelseVilkårType = Optional.ofNullable(MAP_VILKÅR_OMSORGSVILKÅR.get(dto.getVilkårType()))
            .orElseThrow(() -> new FunksjonellException("FP-765341", "Mangler vilkårtype", "Oppgi vilkårtype"));
        var oppdatertOverstyrtHendelse = familieHendelseTjeneste.opprettBuilderForOverstyring(behandlingId);
        oppdatertOverstyrtHendelse.medAdopsjon(oppdatertOverstyrtHendelse.getAdopsjonBuilder()
            .medOmsorgovertalseVilkårType(omsorgsovertakelseVilkårType)
            .medOmsorgsovertakelseDato(dto.getOmsorgsovertakelseDato()));
        familieHendelseTjeneste.lagreOverstyrtHendelse(behandlingId, oppdatertOverstyrtHendelse);
    }

    private void settNyttVilkårOgAvbrytAndreOmsorgsovertakelseVilkårOgAksjonspunkter(AvklarFaktaForOmsorgOgForeldreansvarAksjonspunktDto dto,
                                                                                     OppdateringResultat.Builder builder,
                                                                                     Long behandlingId) {

        // Omsorgsovertakelse
        var vilkårType = dto.getVilkårType();
        if (vilkårType == null || VilkårType.UDEFINERT.equals(vilkårType)) {
            throw new FunksjonellException("FP-381974", "Vilkår ikke valgt", "Velg hvilket vilkår som kommer til anvendelse");
        }

        builder.leggTilIkkeVurdertVilkår(vilkårType);
        // Rydd opp i eventuelle omsorgsvilkår som er tidligere lagt til
        behandlingsresultatRepository.hentHvisEksisterer(behandlingId)
            .ifPresent(br -> br.getVilkårResultat()
                .getVilkårene()
                .stream()
                .filter(vilkår -> OmsorgsvilkårKonfigurasjon.OMSORGS_VILKÅR.contains(vilkår.getVilkårType()))
                // Men uten å fjerne seg selv
                .filter(vilkår -> !vilkår.getVilkårType().equals(vilkårType))
                .forEach(fjernet -> builder.fjernVilkårType(fjernet.getVilkårType())));

        // Avbryt eksisterende aksjonspunkt innen omsorg og foreldreansvar. Dette aksjonspunktet velger vilkår og derfra kommer rett aksjonspunkt
        behandlingRepository.hentBehandling(behandlingId)
            .getAksjonspunkter()
            .stream()
            .filter(ap -> OmsorgsvilkårKonfigurasjon.OMSORGS_AKSJONSPUNKT.contains(ap.getAksjonspunktDefinisjon()))
            .forEach(ap -> builder.medEkstraAksjonspunktResultat(ap.getAksjonspunktDefinisjon(), AksjonspunktStatus.AVBRUTT));
    }

    private SkjermlenkeType getSkjermlenkeType(FagsakYtelseType fagsakYtelseType) {
        return FagsakYtelseType.ENGANGSTØNAD.equals(
            fagsakYtelseType) ? SkjermlenkeType.FAKTA_OM_OMSORG_OG_FORELDREANSVAR : SkjermlenkeType.FAKTA_FOR_OMSORG;
    }

    private OmsorgsovertakelseVilkårType finnTekstBasertPåOmsorgsvilkår(VilkårType vilkårType) {
        if (VilkårType.OMSORGSVILKÅRET.equals(vilkårType)) {
            return OmsorgsovertakelseVilkårType.OMSORGSVILKÅRET;
        }
        if (VilkårType.FORELDREANSVARSVILKÅRET_2_LEDD.equals(vilkårType)) {
            return OmsorgsovertakelseVilkårType.FORELDREANSVARSVILKÅRET_2_LEDD;
        }
        if (VilkårType.FORELDREANSVARSVILKÅRET_4_LEDD.equals(vilkårType)) {
            return OmsorgsovertakelseVilkårType.FORELDREANSVARSVILKÅRET_4_LEDD;
        }
        return null;
    }

    private Optional<LocalDate> getOriginalOmsorgsovertakelseDato(FamilieHendelseGrunnlagEntitet grunnlag) {
        return grunnlag.getGjeldendeAdopsjon().map(AdopsjonEntitet::getOmsorgsovertakelseDato);
    }
}
