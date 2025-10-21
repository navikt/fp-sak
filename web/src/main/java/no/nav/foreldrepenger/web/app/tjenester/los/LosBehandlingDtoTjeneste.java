package no.nav.foreldrepenger.web.app.tjenester.los;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.Skjæringstidspunkt;
import no.nav.foreldrepenger.behandling.revurdering.ytelse.UttakInputTjeneste;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsak;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.Aksjonspunkt;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.OppgittAnnenPartEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.RelasjonsRolleType;
import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.SvangerskapspengerRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.SvpGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.SvpTilretteleggingEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.SvpTilretteleggingerEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.TilretteleggingFOM;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.AvklarteUttakDatoerEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelseFordelingAggregat;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittFordelingEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.årsak.OverføringÅrsak;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.årsak.UtsettelseÅrsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakEgenskapRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.fagsak.egenskaper.FagsakMarkering;
import no.nav.foreldrepenger.behandlingslager.geografisk.MapRegionLandkoder;
import no.nav.foreldrepenger.behandlingslager.geografisk.Region;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektsmeldingTjeneste;
import no.nav.foreldrepenger.domene.iay.modell.Inntektsmelding;
import no.nav.foreldrepenger.domene.iay.modell.Refusjon;
import no.nav.foreldrepenger.domene.personopplysning.PersonopplysningTjeneste;
import no.nav.foreldrepenger.domene.risikoklassifisering.tjeneste.RisikovurderingTjeneste;
import no.nav.foreldrepenger.domene.typer.Beløp;
import no.nav.foreldrepenger.domene.uttak.uttaksgrunnlag.fp.EndringsdatoRevurderingUtleder;
import no.nav.foreldrepenger.domene.ytelsefordeling.YtelseFordelingTjeneste;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktTjeneste;
import no.nav.vedtak.hendelser.behandling.Aksjonspunktstatus;
import no.nav.vedtak.hendelser.behandling.AktørId;
import no.nav.vedtak.hendelser.behandling.Behandlingsstatus;
import no.nav.vedtak.hendelser.behandling.Behandlingstype;
import no.nav.vedtak.hendelser.behandling.Behandlingsårsak;
import no.nav.vedtak.hendelser.behandling.Kildesystem;
import no.nav.vedtak.hendelser.behandling.Ytelse;
import no.nav.vedtak.hendelser.behandling.los.LosBehandlingDto;

/**
 * Returnerer behandlingsinformasjon tilpasset behov i FP-LOS
 *
 */

@ApplicationScoped
public class LosBehandlingDtoTjeneste {

    private YtelseFordelingTjeneste ytelseFordelingTjeneste;
    private RisikovurderingTjeneste risikovurderingTjeneste;
    private InntektsmeldingTjeneste inntektsmeldingTjeneste;
    private FagsakEgenskapRepository fagsakEgenskapRepository;
    private SkjæringstidspunktTjeneste skjæringstidspunktTjeneste;
    private SvangerskapspengerRepository svangerskapspengerRepository;
    private PersonopplysningTjeneste personopplysningTjeneste;
    private UttakInputTjeneste uttakInputTjeneste;
    private EndringsdatoRevurderingUtleder endringsdatoRevurderingUtleder;


    @Inject
    public LosBehandlingDtoTjeneste(YtelseFordelingTjeneste ytelseFordelingTjeneste,
                                    RisikovurderingTjeneste risikovurderingTjeneste,
                                    InntektsmeldingTjeneste inntektsmeldingTjeneste,
                                    SkjæringstidspunktTjeneste skjæringstidspunktTjeneste,
                                    SvangerskapspengerRepository svangerskapspengerRepository,
                                    FagsakEgenskapRepository fagsakEgenskapRepository,
                                    PersonopplysningTjeneste personopplysningTjeneste,
                                    UttakInputTjeneste uttakInputTjeneste,
                                    @FagsakYtelseTypeRef(FagsakYtelseType.FORELDREPENGER) EndringsdatoRevurderingUtleder endringsdatoRevurderingUtleder) {
        this.ytelseFordelingTjeneste = ytelseFordelingTjeneste;
        this.risikovurderingTjeneste = risikovurderingTjeneste;
        this.inntektsmeldingTjeneste = inntektsmeldingTjeneste;
        this.skjæringstidspunktTjeneste = skjæringstidspunktTjeneste;
        this.fagsakEgenskapRepository = fagsakEgenskapRepository;
        this.svangerskapspengerRepository = svangerskapspengerRepository;
        this.uttakInputTjeneste = uttakInputTjeneste;
        this.personopplysningTjeneste = personopplysningTjeneste;
        this.endringsdatoRevurderingUtleder = endringsdatoRevurderingUtleder;
    }

    LosBehandlingDtoTjeneste() {
        //for CDI proxy
    }

    public LosBehandlingDto lagLosBehandlingDto(Behandling behandling, boolean harInnhentetRegisterData) {

        var faresignaler = harInnhentetRegisterData && mapFaresignaler(behandling);
        var refusjonskrav = harNærFullRefusjonAlleIM(behandling);
        var refusjonegenskap = refusjonskrav ? BehandlingEgenskap.REFUSJONSKRAV : BehandlingEgenskap.DIREKTE_UTBETALING;
        List<String> behandlingsegenskaper = new ArrayList<>(List.of(refusjonegenskap.name()));
        if (faresignaler) behandlingsegenskaper.add(BehandlingEgenskap.FARESIGNALER.name());
        if (farForeldrepengerUtenMorEllerMorRAV(behandling)) behandlingsegenskaper.add(BehandlingEgenskap.MOR_UKJENT_UTLAND.name());
        var fpUttak = mapForeldrepengerUttak(behandling, behandlingsegenskaper);


        return new LosBehandlingDto(behandling.getUuid(),
            Kildesystem.FPSAK,
            behandling.getSaksnummer().getVerdi(),
            mapYtelse(behandling),
            new AktørId(behandling.getAktørId().getId()),
            mapBehandlingstype(behandling),
            mapBehandlingsstatus(behandling),
            behandling.getOpprettetTidspunkt(),
            behandling.getBehandlendeEnhet(),
            behandling.getBehandlingstidFrist(),
            behandling.getAnsvarligSaksbehandler(),
            mapAksjonspunkter(behandling),
            mapBehandlingsårsaker(behandling).stream().toList(),
            faresignaler,
            refusjonskrav,
            lagFagsakEgenskaperString(behandling.getFagsak()),
            fpUttak,
            behandlingsegenskaper,
            null);
    }

    private static Ytelse mapYtelse(Behandling behandling) {
        return switch (behandling.getFagsakYtelseType()) {
            case FORELDREPENGER -> Ytelse.FORELDREPENGER;
            case ENGANGSTØNAD -> Ytelse.ENGANGSTØNAD;
            case SVANGERSKAPSPENGER -> Ytelse.SVANGERSKAPSPENGER;
            case UDEFINERT -> throw new IllegalStateException("Sak uten kjent ytelse");
        };
    }

    private static Behandlingstype mapBehandlingstype(Behandling behandling) {
        return switch (behandling.getType()) {
            case FØRSTEGANGSSØKNAD -> Behandlingstype.FØRSTEGANGS;
            case REVURDERING -> Behandlingstype.REVURDERING;
            case ANKE -> Behandlingstype.ANKE;
            case KLAGE -> Behandlingstype.KLAGE;
            case INNSYN -> Behandlingstype.INNSYN;
            default-> throw new IllegalStateException("Behandling uten kjent type");
        };
    }

    private static Behandlingsstatus mapBehandlingsstatus(Behandling behandling) {
        return switch (behandling.getStatus()) {
            case OPPRETTET -> Behandlingsstatus.OPPRETTET;
            case UTREDES -> Behandlingsstatus.UTREDES;
            case FATTER_VEDTAK -> Behandlingsstatus.FATTER_VEDTAK;
            case IVERKSETTER_VEDTAK -> Behandlingsstatus.IVERKSETTER_VEDTAK;
            case AVSLUTTET -> Behandlingsstatus.AVSLUTTET;
        };
    }

    private static Set<Behandlingsårsak> mapBehandlingsårsaker(Behandling behandling) {
        Set<Behandlingsårsak> årsaker = new HashSet<>();
        var opprinneligeÅrsaker = behandling.getBehandlingÅrsaker().stream()
            .map(BehandlingÅrsak::getBehandlingÅrsakType)
            .collect(Collectors.toSet());
        opprinneligeÅrsaker.stream()
            .map(LosBehandlingDtoTjeneste::mapBehandlingsårsak)
            .filter(behandlingsårsak -> !Behandlingsårsak.ANNET.equals(behandlingsårsak))
            .forEach(årsaker::add);
        if (BehandlingType.REVURDERING.equals(behandling.getType())) {
            if (behandling.getBehandlingÅrsaker().stream().anyMatch(BehandlingÅrsak::erManueltOpprettet)) {
                årsaker.add(Behandlingsårsak.MANUELL);
            }
            if (opprinneligeÅrsaker.stream().allMatch(BehandlingÅrsakType.RE_ENDRET_INNTEKTSMELDING::equals)) {
                årsaker.add(Behandlingsårsak.INNTEKTSMELDING);
            }
        } else if (BehandlingType.FØRSTEGANGSSØKNAD.equals(behandling.getType())) {
            årsaker.add(Behandlingsårsak.SØKNAD);
        }
        return årsaker;
    }

    private static Behandlingsårsak mapBehandlingsårsak(BehandlingÅrsakType årsak) {
        return switch (årsak) {
            case BERØRT_BEHANDLING, REBEREGN_FERIEPENGER, ENDRE_DEKNINGSGRAD -> Behandlingsårsak.BERØRT;
            case RE_ENDRING_FRA_BRUKER -> Behandlingsårsak.SØKNAD;
            case RE_VEDTAK_PLEIEPENGER -> Behandlingsårsak.PLEIEPENGER;
            case RE_MANGLER_FØDSEL, RE_MANGLER_FØDSEL_I_PERIODE, RE_AVVIK_ANTALL_BARN -> Behandlingsårsak.ETTERKONTROLL;
            case ETTER_KLAGE, RE_KLAGE_MED_END_INNTEKT, RE_KLAGE_UTEN_END_INNTEKT -> Behandlingsårsak.KLAGE_OMGJØRING;
            case KLAGE_TILBAKEBETALING -> Behandlingsårsak.KLAGE_TILBAKEBETALING;
            case RE_SATS_REGULERING -> Behandlingsårsak.REGULERING;
            case RE_UTSATT_START -> Behandlingsårsak.UTSATT_START;
            case OPPHØR_YTELSE_NYTT_BARN -> Behandlingsårsak.OPPHØR_NY_SAK;
            default -> Behandlingsårsak.ANNET;
        };
    }

    private static List<LosBehandlingDto.LosAksjonspunktDto> mapAksjonspunkter(Behandling behandling) {
        return behandling.getAksjonspunkter().stream()
            .map(LosBehandlingDtoTjeneste::mapTilLosAksjonspunkt)
            .toList();
    }

    private static LosBehandlingDto.LosAksjonspunktDto mapTilLosAksjonspunkt(Aksjonspunkt aksjonspunkt) {
        return new LosBehandlingDto.LosAksjonspunktDto(aksjonspunkt.getAksjonspunktDefinisjon().getKode(),
            mapAksjonspunktstatus(aksjonspunkt), aksjonspunkt.getFristTid());
    }

    private static Aksjonspunktstatus mapAksjonspunktstatus(Aksjonspunkt aksjonspunkt) {
        return switch (aksjonspunkt.getStatus()) {
            case OPPRETTET -> Aksjonspunktstatus.OPPRETTET;
            case UTFØRT -> Aksjonspunktstatus.UTFØRT;
            case AVBRUTT -> Aksjonspunktstatus.AVBRUTT;
        };
    }

    private boolean mapFaresignaler(Behandling behandling) {
        return BehandlingType.FØRSTEGANGSSØKNAD.equals(behandling.getType()) &&
            (behandling.harAksjonspunktMedType(AksjonspunktDefinisjon.VURDER_FARESIGNALER) ||
                risikovurderingTjeneste.skalVurdereFaresignaler(BehandlingReferanse.fra(behandling)));
    }

    private LosBehandlingDto.LosForeldrepengerDto mapForeldrepengerUttak(Behandling behandling, List<String> behandlingsegenskaper) {
        if (!behandling.erYtelseBehandling()) {
            return null;
        }
        var aggregat = ytelseFordelingTjeneste.hentAggregatHvisEksisterer(behandling.getId());
        var vurderSykdom = aggregat.map(YtelseFordelingAggregat::getGjeldendeFordeling).map(OppgittFordelingEntitet::getPerioder).orElse(List.of())
            .stream().anyMatch(LosBehandlingDtoTjeneste::periodeGjelderSykdom);
        if (vurderSykdom) {
            behandlingsegenskaper.add(BehandlingEgenskap.SYKDOMSVURDERING.name());
        }
        if (FagsakYtelseType.UDEFINERT.equals(behandling.getFagsakYtelseType()) || !behandling.erYtelseBehandling()) {
            return null;
        }
        if (FagsakYtelseType.ENGANGSTØNAD.equals(behandling.getFagsakYtelseType()) || BehandlingType.FØRSTEGANGSSØKNAD.equals(behandling.getType())) {
            var uttakEllerSkjæringstidspunkt = finnUttakEllerUtledetSkjæringstidspunkt(behandling);
            return new LosBehandlingDto.LosForeldrepengerDto(uttakEllerSkjæringstidspunkt);
        } else { // Revudering SVP eller FP
            var endretUttakFom = FagsakYtelseType.FORELDREPENGER.equals(behandling.getFagsakYtelseType()) ?
                finnEndringsdatoForeldrepengerRevurdering(behandling, aggregat) : finnEndringsdatoSvangerskapspengerRevurdering(behandling);
            var endringEllerFørsteUttak = endretUttakFom.orElseGet(() -> finnUttakEllerUtledetSkjæringstidspunkt(behandling));
            return new LosBehandlingDto.LosForeldrepengerDto(endringEllerFørsteUttak);
        }
    }

    private LocalDate finnUttakEllerUtledetSkjæringstidspunkt(Behandling behandling) {
        try {
            var skjæringstidspunkt = skjæringstidspunktTjeneste.getSkjæringstidspunkter(behandling.getId());
            return Optional.ofNullable(skjæringstidspunkt).flatMap(Skjæringstidspunkt::getFørsteUttaksdatoSøknad)
                .or(() -> Optional.ofNullable(skjæringstidspunkt).flatMap(Skjæringstidspunkt::getSkjæringstidspunktHvisUtledet))
                .orElse(null);
        } catch (Exception e) {
            return null;
        }
    }

    private Optional<LocalDate> finnEndringsdatoForeldrepengerRevurdering(Behandling behandling, Optional<YtelseFordelingAggregat> aggregat) {
        try {
            return aggregat.flatMap(YtelseFordelingAggregat::getAvklarteDatoer).map(AvklarteUttakDatoerEntitet::getGjeldendeEndringsdato)
                .or(() -> Optional.ofNullable(endringsdatoRevurderingUtleder.utledEndringsdato(uttakInputTjeneste.lagInput(behandling))));
        } catch (Exception e) {
            return Optional.empty();
        }

    }

    private Optional<LocalDate> finnEndringsdatoSvangerskapspengerRevurdering(Behandling behandling) {
        if (!behandling.harBehandlingÅrsak(BehandlingÅrsakType.RE_ENDRING_FRA_BRUKER)) {
            return Optional.empty();
        }
        return svangerskapspengerRepository.hentGrunnlag(behandling.getId()).map(SvpGrunnlagEntitet::getGjeldendeVersjon)
            .map(SvpTilretteleggingerEntitet::getTilretteleggingListe).orElse(List.of()).stream()
            .filter(te -> !te.getKopiertFraTidligereBehandling() && te.getSkalBrukes())
            .map(SvpTilretteleggingEntitet::getTilretteleggingFOMListe)
            .flatMap(Collection::stream)
            .map(TilretteleggingFOM::getFomDato)
            .min(Comparator.naturalOrder());
    }

    private static boolean periodeGjelderSykdom(OppgittPeriodeEntitet periode) {
        var årsak = periode.getÅrsak();
        var utsettelseSykdom = periode.isUtsettelse() && (UtsettelseÅrsak.SYKDOM.equals(årsak) ||
            UtsettelseÅrsak.INSTITUSJON_BARN.equals(årsak) || UtsettelseÅrsak.INSTITUSJON_SØKER.equals(årsak));
        var overføringSykdom = periode.isOverføring() && (OverføringÅrsak.SYKDOM_ANNEN_FORELDER.equals(årsak) ||
            OverføringÅrsak.INSTITUSJONSOPPHOLD_ANNEN_FORELDER.equals(årsak));
        return utsettelseSykdom || overføringSykdom;
    }

    private boolean farForeldrepengerUtenMorEllerMorRAV(Behandling behandling) {
        if (!FagsakYtelseType.FORELDREPENGER.equals(behandling.getFagsakYtelseType()) || RelasjonsRolleType.MORA.equals(behandling.getRelasjonsRolleType())) {
            return false;
        }
        if (!behandling.erYtelseBehandling()) {
            return false;
        }
        var annenpart = personopplysningTjeneste.hentOppgittAnnenPart(BehandlingReferanse.fra(behandling));
        var annenpartBosattRAV = annenpart
            .filter(a -> a.getAktørId() == null)
            .map(OppgittAnnenPartEntitet::getUtenlandskFnrLand)
            .map(MapRegionLandkoder::mapLandkode)
            .filter(r -> !Region.NORDEN.equals(r) && !Region.EOS.equals(r))
            .isPresent();
        return annenpart.isEmpty() || annenpartBosattRAV;
    }

    private boolean harNærFullRefusjonAlleIM(Behandling behandling) {
        if (FagsakYtelseType.ENGANGSTØNAD.equals(behandling.getFagsakYtelseType()) || !behandling.erYtelseBehandling() || behandling.harÅpentAksjonspunktMedType(AksjonspunktDefinisjon.VURDER_ARBEIDSFORHOLD_INNTEKTSMELDING)) {
            return false;
        }
        var inntektsmeldinger = inntektsmeldingTjeneste.hentAlleInntektsmeldingerForAngitteBehandlinger(Set.of(behandling.getId()));
        return !inntektsmeldinger.isEmpty() && inntektsmeldinger.stream().allMatch(LosBehandlingDtoTjeneste::harNærFullRefusjon);
    }

    // Regner refusjonsandel over 90% som full refusjon, ellers som direkte utbetaling
    private static boolean harNærFullRefusjon(Inntektsmelding inntektsmelding) {
        var inntekt = Optional.ofNullable(inntektsmelding.getInntektBeløp()).map(Beløp::getVerdi).orElse(BigDecimal.ZERO);
        var refusjon = Optional.ofNullable(inntektsmelding.getRefusjonBeløpPerMnd()).map(Beløp::getVerdi).orElse(BigDecimal.ZERO);
        var refusjoner = Optional.ofNullable(inntektsmelding.getEndringerRefusjon()).orElseGet(List::of).stream()
            .map(Refusjon::getRefusjonsbeløp)
            .map(Beløp::getVerdi)
            .max(Comparator.naturalOrder()).orElse(BigDecimal.ZERO);
        var maxrefusjon = refusjon.compareTo(refusjoner) > 0 ? refusjon : refusjoner;
        return inntekt.compareTo(BigDecimal.ZERO) > 0
            && maxrefusjon.multiply(BigDecimal.TEN).compareTo(inntekt.multiply(new BigDecimal(9))) > 0;
    }

    public List<String> lagFagsakEgenskaperString(Fagsak fagsak) {
        return fagsakEgenskapRepository.finnFagsakMarkeringer(fagsak.getId()).stream()
            .map(this::mapLokalMarkering)
            .map(FagsakEgenskap::name)
            .toList();
    }

    private FagsakEgenskap mapLokalMarkering(FagsakMarkering markering) {
        return  switch (markering) {
            case EØS_BOSATT_NORGE -> FagsakEgenskap.EØS_BOSATT_NORGE;
            case BOSATT_UTLAND -> FagsakEgenskap.BOSATT_UTLAND;
            case SAMMENSATT_KONTROLL -> FagsakEgenskap.SAMMENSATT_KONTROLL;
            case DØD_DØDFØDSEL -> FagsakEgenskap.DØD;
            case SELVSTENDIG_NÆRING -> FagsakEgenskap.NÆRING;
            case BARE_FAR_RETT -> FagsakEgenskap.BARE_FAR_RETT;
            case PRAKSIS_UTSETTELSE -> FagsakEgenskap.PRAKSIS_UTSETTELSE;
            case HASTER -> FagsakEgenskap.HASTER;
        };
    }

    // Bør matche LOS sin LokalFagsakEgenskap 1:1
    public enum FagsakEgenskap {
        EØS_BOSATT_NORGE, BOSATT_UTLAND, SAMMENSATT_KONTROLL, DØD, NÆRING, BARE_FAR_RETT, PRAKSIS_UTSETTELSE, HASTER;

    }

    public enum BehandlingEgenskap {
        SYKDOMSVURDERING, MOR_UKJENT_UTLAND, FARESIGNALER, DIREKTE_UTBETALING, REFUSJONSKRAV
    }

}
