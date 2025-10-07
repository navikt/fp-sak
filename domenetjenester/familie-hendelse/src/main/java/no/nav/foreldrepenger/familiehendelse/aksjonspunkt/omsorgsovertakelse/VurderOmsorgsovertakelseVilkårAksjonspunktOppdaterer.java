package no.nav.foreldrepenger.familiehendelse.aksjonspunkt.omsorgsovertakelse;

import static no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagLinjeBuilder.fraTilEquals;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterer;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdateringTransisjon;
import no.nav.foreldrepenger.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.OmsorgsovertakelseVilkårType;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.UidentifisertBarn;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.UidentifisertBarnEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkAktør;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.Historikkinnslag;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.skjermlenke.SkjermlenkeType;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.Avslagsårsak;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårUtfallType;
import no.nav.foreldrepenger.familiehendelse.FamilieHendelseTjeneste;
import no.nav.foreldrepenger.familiehendelse.aksjonspunkt.omsorgsovertakelse.dto.VurderOmsorgsovertakelseVilkårAksjonspunktDto;
import no.nav.foreldrepenger.skjæringstidspunkt.OpplysningsPeriodeTjeneste;

/**
 * Håndterer oppdatering av adopsjon/omsorgsvilkåret.
 */
@ApplicationScoped
@DtoTilServiceAdapter(dto = VurderOmsorgsovertakelseVilkårAksjonspunktDto.class, adapter = AksjonspunktOppdaterer.class)
public class VurderOmsorgsovertakelseVilkårAksjonspunktOppdaterer implements AksjonspunktOppdaterer<VurderOmsorgsovertakelseVilkårAksjonspunktDto> {

    private static final Set<AksjonspunktDefinisjon> LEGACY_AKSJONSPUNKT = Set.of(AksjonspunktDefinisjon.AVKLAR_ADOPSJONSDOKUMENTAJON,
        AksjonspunktDefinisjon.AVKLAR_OM_ADOPSJON_GJELDER_EKTEFELLES_BARN, AksjonspunktDefinisjon.AVKLAR_OM_SØKER_ER_MANN_SOM_ADOPTERER_ALENE,
        AksjonspunktDefinisjon.AVKLAR_VILKÅR_FOR_OMSORGSOVERTAKELSE, AksjonspunktDefinisjon.AVKLAR_VILKÅR_FOR_FORELDREANSVAR,
        AksjonspunktDefinisjon.MANUELL_VURDERING_AV_OMSORGSVILKÅRET,
        AksjonspunktDefinisjon.MANUELL_VURDERING_AV_FORELDREANSVARSVILKÅRET_2_LEDD,
        AksjonspunktDefinisjon.MANUELL_VURDERING_AV_FORELDREANSVARSVILKÅRET_4_LEDD);
    private static final Set<VilkårType> LEGACY_VILKÅR = Set.of(VilkårType.ADOPSJONSVILKÅRET_ENGANGSSTØNAD, VilkårType.ADOPSJONSVILKARET_FORELDREPENGER,
        VilkårType.OMSORGSVILKÅRET, VilkårType.FORELDREANSVARSVILKÅRET_2_LEDD, VilkårType.FORELDREANSVARSVILKÅRET_4_LEDD);

    private HistorikkinnslagRepository historikkinnslagRepository;
    private BehandlingRepository behandlingRepository;
    private VilkårResultatRepository resultatRepository;
    private FamilieHendelseTjeneste familieHendelseTjeneste;
    private OpplysningsPeriodeTjeneste opplysningsPeriodeTjeneste;

    protected VurderOmsorgsovertakelseVilkårAksjonspunktOppdaterer() {
        // for CDI proxy
    }

    @Inject
    public VurderOmsorgsovertakelseVilkårAksjonspunktOppdaterer(HistorikkinnslagRepository historikkinnslagRepository,
                                                                BehandlingRepository behandlingRepository,
                                                                VilkårResultatRepository resultatRepository,
                                                                FamilieHendelseTjeneste familieHendelseTjeneste,
                                                                OpplysningsPeriodeTjeneste opplysningsPeriodeTjeneste) {
        this.historikkinnslagRepository = historikkinnslagRepository;
        this.behandlingRepository = behandlingRepository;
        this.resultatRepository = resultatRepository;
        this.familieHendelseTjeneste = familieHendelseTjeneste;
        this.opplysningsPeriodeTjeneste = opplysningsPeriodeTjeneste;
    }

    @Override
    public OppdateringResultat oppdater(VurderOmsorgsovertakelseVilkårAksjonspunktDto dto, AksjonspunktOppdaterParameter param) {
        var avslagskode = dto.getAvslagskode();
        if (avslagskode != null && !VilkårType.OMSORGSOVERTAKELSEVILKÅR.getAvslagsårsaker().contains(avslagskode)) {
            throw new IllegalArgumentException("Ugyldig avslagsårsak for adopsjon/omsorgsvilkåret");
        }
        var delvilkår = dto.getDelvilkår();
        if (delvilkår == null || OmsorgsovertakelseVilkårType.UDEFINERT.equals(delvilkår)) {
            throw new IllegalArgumentException("Ikke valgt delvilkår under adopsjon/omsorgsvilkåret");
        }

        var ref = param.getRef();
        var behandling = behandlingRepository.hentBehandling(ref.behandlingId());
        var forrigeFikspunkt = opplysningsPeriodeTjeneste.utledFikspunktForRegisterInnhenting(behandling.getId(), param.getRef().fagsakYtelseType());

        var utfall = oppdaterAdopsjonOmsorg(ref, avslagskode, delvilkår, dto);

        var resultatBuilder = OppdateringResultat.utenTransisjon();
        // Midlertidig sjekk + fjerning av legacy aksjonspunkter og vilkår
        behandling.getÅpneAksjonspunkter(LEGACY_AKSJONSPUNKT)
            .forEach(ap -> resultatBuilder.medEkstraAksjonspunktResultat(ap.getAksjonspunktDefinisjon(), AksjonspunktStatus.AVBRUTT));
        resultatRepository.hentHvisEksisterer(behandling.getId()).map(VilkårResultat::getVilkårene).orElseGet(List::of).stream()
            .filter(v -> LEGACY_VILKÅR.contains(v.getVilkårType()))
            .forEach(fjernet -> resultatBuilder.fjernVilkårType(fjernet.getVilkårType()));

        if (VilkårUtfallType.OPPFYLT.equals(utfall)) {
            var sistefikspunkt = opplysningsPeriodeTjeneste.utledFikspunktForRegisterInnhenting(behandling.getId(), param.getRef().fagsakYtelseType());
            if (!Objects.equals(forrigeFikspunkt, sistefikspunkt)) {
                resultatBuilder.medOppdaterGrunnlag();
            }
            return resultatBuilder.leggTilManueltOppfyltVilkår(VilkårType.OMSORGSOVERTAKELSEVILKÅR).build();
        } else {
            return resultatBuilder
                .medFremoverHopp(AksjonspunktOppdateringTransisjon.AVSLAG_VILKÅR)
                .leggTilManueltAvslåttVilkår(VilkårType.OMSORGSOVERTAKELSEVILKÅR, avslagskode)
                .build();
        }


    }

    public VilkårUtfallType oppdaterAdopsjonOmsorg(BehandlingReferanse ref,
                                                   Avslagsårsak avslagsårsak,
                                                   OmsorgsovertakelseVilkårType delvilkår,
                                                   VurderOmsorgsovertakelseVilkårAksjonspunktDto dto) {
        var utfall = avslagsårsak == null ? VilkårUtfallType.OPPFYLT : VilkårUtfallType.IKKE_OPPFYLT;
        var omsorgsovertakelsedato = dto.getOmsorgsovertakelseDato();
        var fødselsdatoer = dto.getFødselsdatoer();
        var ektefellesBarn = dto.getEktefellesBarn();

        var grunnlag = familieHendelseTjeneste.hentAggregat(ref.behandlingId());
        var gjeldendeAdopsjon = grunnlag.getGjeldendeAdopsjon().orElseThrow();
        var gjeldendeBarn = Optional.ofNullable(grunnlag.getGjeldendeBarna()).orElseGet(List::of).stream()
            .collect(Collectors.toMap(UidentifisertBarn::getBarnNummer, UidentifisertBarn::getFødselsdato));

        var oppdatertOverstyrtHendelse = familieHendelseTjeneste.opprettBuilderForOverstyring(ref.behandlingId());
        oppdatertOverstyrtHendelse.tilbakestillBarn()
            .medAntallBarn(dto.getFødselsdatoer().size())
            .medAdopsjon(oppdatertOverstyrtHendelse.getAdopsjonBuilder()
                .medOmsorgsovertakelseDato(omsorgsovertakelsedato)
                .medOmsorgovertalseVilkårType(delvilkår)
                .medErEktefellesBarn(ektefellesBarn));
        fødselsdatoer.forEach((barnnummer, fødselsdato) -> oppdatertOverstyrtHendelse.leggTilBarn(new UidentifisertBarnEntitet(fødselsdato, barnnummer)));
        familieHendelseTjeneste.lagreOverstyrtHendelse(ref.behandlingId(), oppdatertOverstyrtHendelse);


        var historikkinnslagBuilder = new Historikkinnslag.Builder()
            .medAktør(HistorikkAktør.SAKSBEHANDLER)
            .medFagsakId(ref.fagsakId())
            .medBehandlingId(ref.behandlingId())
            .medTittel(SkjermlenkeType.FAKTA_OM_OMSORGSOVERTAKELSE)
            .addLinje(fraTilEquals("Adopsjons- og omsorgsvilkåret", null, utfall))
            .addLinje(fraTilEquals("Delvilkår", gjeldendeAdopsjon.getOmsorgovertakelseVilkår(), delvilkår))
            .addLinje(fraTilEquals("Omsorgsovertakelsesdato", gjeldendeAdopsjon.getOmsorgsovertakelseDato(), dto.getOmsorgsovertakelseDato()))
            .addLinje(fraTilEquals("Ektefelles barn", gjeldendeAdopsjon.getErEktefellesBarn(), ektefellesBarn));
        for (int i = 0; i < Math.max(fødselsdatoer.size(), gjeldendeBarn.size()); i++) {
            var gjeldende = gjeldendeBarn.get(i);
            var oppdatert = fødselsdatoer.get(i);
            historikkinnslagBuilder.addLinje(fraTilEquals("Fødselsdato", gjeldende, oppdatert));
        }
        var historikkinnslag = historikkinnslagBuilder.addLinje(dto.getBegrunnelse()).build();
        historikkinnslagRepository.lagre(historikkinnslag);

        return utfall;
    }

}
