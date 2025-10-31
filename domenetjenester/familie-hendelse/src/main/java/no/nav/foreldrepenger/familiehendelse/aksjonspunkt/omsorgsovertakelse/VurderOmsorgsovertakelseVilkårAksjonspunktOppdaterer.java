package no.nav.foreldrepenger.familiehendelse.aksjonspunkt.omsorgsovertakelse;

import static no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagLinjeBuilder.fraTilEquals;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterer;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdateringTransisjon;
import no.nav.foreldrepenger.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.AdopsjonEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.OmsorgsovertakelseVilkårType;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.UidentifisertBarn;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.UidentifisertBarnEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkAktør;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.Historikkinnslag;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.skjermlenke.SkjermlenkeType;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.Avslagsårsak;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårUtfallType;
import no.nav.foreldrepenger.familiehendelse.FamilieHendelseTjeneste;
import no.nav.foreldrepenger.familiehendelse.aksjonspunkt.omsorgsovertakelse.dto.OmsorgsovertakelseBarnDto;
import no.nav.foreldrepenger.familiehendelse.aksjonspunkt.omsorgsovertakelse.dto.VurderOmsorgsovertakelseVilkårAksjonspunktDto;
import no.nav.foreldrepenger.skjæringstidspunkt.OpplysningsPeriodeTjeneste;
import no.nav.vedtak.exception.FunksjonellException;

/**
 * Håndterer oppdatering av adopsjon/omsorgsvilkåret.
 */
@ApplicationScoped
@DtoTilServiceAdapter(dto = VurderOmsorgsovertakelseVilkårAksjonspunktDto.class, adapter = AksjonspunktOppdaterer.class)
public class VurderOmsorgsovertakelseVilkårAksjonspunktOppdaterer implements AksjonspunktOppdaterer<VurderOmsorgsovertakelseVilkårAksjonspunktDto> {

    private HistorikkinnslagRepository historikkinnslagRepository;
    private BehandlingRepository behandlingRepository;
    private FamilieHendelseTjeneste familieHendelseTjeneste;
    private OpplysningsPeriodeTjeneste opplysningsPeriodeTjeneste;
    private OmsorgsovertakelseVilkårTypeUtleder delvilkårUtleder;

    protected VurderOmsorgsovertakelseVilkårAksjonspunktOppdaterer() {
        // for CDI proxy
    }

    @Inject
    public VurderOmsorgsovertakelseVilkårAksjonspunktOppdaterer(BehandlingRepositoryProvider repositoryProvider,
                                                                FamilieHendelseTjeneste familieHendelseTjeneste,
                                                                OpplysningsPeriodeTjeneste opplysningsPeriodeTjeneste,
                                                                OmsorgsovertakelseVilkårTypeUtleder delvilkårUtleder) {
        this.historikkinnslagRepository = repositoryProvider.getHistorikkinnslagRepository();
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.familieHendelseTjeneste = familieHendelseTjeneste;
        this.opplysningsPeriodeTjeneste = opplysningsPeriodeTjeneste;
        this.delvilkårUtleder = delvilkårUtleder;
    }

    @Override
    public OppdateringResultat oppdater(VurderOmsorgsovertakelseVilkårAksjonspunktDto dto, AksjonspunktOppdaterParameter param) {
        if (dto.getBarn().isEmpty() && dto.getAvslagskode() == null) {
            throw new FunksjonellException("FP-765324", "Vilkår oppfylt uten barn", "Du må velge minst ett barn eller avslå.");
        }
        var delvilkår = dto.getDelvilkår();
        if (delvilkår == null || OmsorgsovertakelseVilkårType.UDEFINERT.equals(delvilkår)) {
            throw new FunksjonellException("FP-765324", "Ikke valgt delvilkår", "Du må velge ett av undervilkårene." );
        }
        if ((OmsorgsovertakelseVilkårType.FP_ADOPSJONSVILKÅRET.equals(delvilkår) && dto.getEktefellesBarn())
            || (OmsorgsovertakelseVilkårType.FP_STEBARNSADOPSJONSVILKÅRET.equals(delvilkår) && !dto.getEktefellesBarn())) {
            throw new FunksjonellException("FP-765324", "Ugyldig kombinasjon av undervilkår og ektefelles barn",
                "Du må velge Nei for ektefelles barn ved adopsjon og Ja for stebarnsadopsjon.");
        }
        var avslagskode = dto.getAvslagskode();
        if (avslagskode != null && !delvilkår.getAvslagsårsaker().contains(avslagskode)) {
            throw new IllegalArgumentException("Ugyldig avslagsårsak for omsorgsovertakelsesvilkåret");
        }
        var fødselsdatoer = dto.getBarn().stream()
            .collect(Collectors.groupingBy(OmsorgsovertakelseBarnDto::barnNummer, Collectors.toList()));
        if (fødselsdatoer.values().stream().anyMatch(l -> l.size() > 1)) {
            throw new IllegalArgumentException("Duplikate barnNummer i fødselsdatoer for omsorgsovertakelsesvilkåret");
        }

        var ref = param.getRef();
        var behandling = behandlingRepository.hentBehandling(ref.behandlingId());
        var forrigeFikspunkt = opplysningsPeriodeTjeneste.utledFikspunktForRegisterInnhenting(behandling.getId(), param.getRef().fagsakYtelseType());

        var utfall = oppdaterAdopsjonOmsorg(ref, avslagskode, delvilkår, dto);

        var resultatBuilder = OppdateringResultat.utenTransisjon();

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
        var fødselsdatoer = dto.getBarn().stream()
            .collect(Collectors.toMap(OmsorgsovertakelseBarnDto::barnNummer, OmsorgsovertakelseBarnDto::fødselsdato));
        var ektefellesBarn = Objects.equals(dto.getEktefellesBarn(), Boolean.TRUE) || OmsorgsovertakelseVilkårType.FP_STEBARNSADOPSJONSVILKÅRET.equals(dto.getDelvilkår());

        var grunnlag = familieHendelseTjeneste.hentAggregat(ref.behandlingId());
        var gjeldendeAdopsjon = grunnlag.getGjeldendeAdopsjon().orElseThrow();
        var gjeldendeBarn = Optional.ofNullable(grunnlag.getGjeldendeBarna()).orElseGet(List::of).stream()
            .collect(Collectors.toMap(UidentifisertBarn::getBarnNummer, UidentifisertBarn::getFødselsdato));
        var gjeldendeDelvilkår = grunnlag.getOverstyrtVersjon().flatMap(FamilieHendelseEntitet::getAdopsjon)
            .map(AdopsjonEntitet::getOmsorgovertakelseVilkår)
            .or(() -> Optional.of(delvilkårUtleder.utledDelvilkår(ref, grunnlag.getSøknadVersjon())))
            .orElse(OmsorgsovertakelseVilkårType.UDEFINERT);

        var oppdatertOverstyrtHendelse = familieHendelseTjeneste.opprettBuilderForOverstyring(ref.behandlingId());
        oppdatertOverstyrtHendelse.tilbakestillBarn()
            .medAntallBarn(dto.getBarn().size())
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
            .addLinje(fraTilEquals("Delvilkår", gjeldendeDelvilkår, delvilkår))
            .addLinje(fraTilEquals("Omsorgsovertakelsesdato", gjeldendeAdopsjon.getOmsorgsovertakelseDato(), dto.getOmsorgsovertakelseDato()))
            .addLinje(fraTilEquals("Ektefelles barn", gjeldendeAdopsjon.getErEktefellesBarn(), ektefellesBarn))
            .addLinje(fraTilEquals("Antall barn", gjeldendeBarn.size(), fødselsdatoer.size()));
        var maxIndexGjeldende = gjeldendeBarn.keySet().stream().max(Comparator.naturalOrder()).orElse(0);
        var maxIndexOppdatert = fødselsdatoer.keySet().stream().max(Comparator.naturalOrder()).orElse(0);
        var maxIndex = Math.max(maxIndexGjeldende, maxIndexOppdatert) + 1;
        for (int i = 0; i < maxIndex; i++) {
            var gjeldende = gjeldendeBarn.get(i);
            var oppdatert = fødselsdatoer.get(i);
            historikkinnslagBuilder.addLinje(fraTilEquals("Fødselsdato", gjeldende, oppdatert));
        }
        var historikkinnslag = historikkinnslagBuilder.addLinje(dto.getBegrunnelse()).build();
        historikkinnslagRepository.lagre(historikkinnslag);

        return utfall;
    }

}
