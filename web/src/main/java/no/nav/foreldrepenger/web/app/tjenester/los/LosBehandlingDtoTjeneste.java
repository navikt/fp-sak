package no.nav.foreldrepenger.web.app.tjenester.los;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.Skjæringstidspunkt;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsak;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.Aksjonspunkt;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
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
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektsmeldingTjeneste;
import no.nav.foreldrepenger.domene.iay.modell.Inntektsmelding;
import no.nav.foreldrepenger.domene.risikoklassifisering.tjeneste.RisikovurderingTjeneste;
import no.nav.foreldrepenger.domene.typer.Beløp;
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
import no.nav.vedtak.hendelser.behandling.los.LosFagsakEgenskaperDto;

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


    @Inject
    public LosBehandlingDtoTjeneste(YtelseFordelingTjeneste ytelseFordelingTjeneste,
                                    RisikovurderingTjeneste risikovurderingTjeneste,
                                    InntektsmeldingTjeneste inntektsmeldingTjeneste,
                                    SkjæringstidspunktTjeneste skjæringstidspunktTjeneste,
                                    SvangerskapspengerRepository svangerskapspengerRepository,
                                    FagsakEgenskapRepository fagsakEgenskapRepository) {
        this.ytelseFordelingTjeneste = ytelseFordelingTjeneste;
        this.risikovurderingTjeneste = risikovurderingTjeneste;
        this.inntektsmeldingTjeneste = inntektsmeldingTjeneste;
        this.skjæringstidspunktTjeneste = skjæringstidspunktTjeneste;
        this.fagsakEgenskapRepository = fagsakEgenskapRepository;
        this.svangerskapspengerRepository = svangerskapspengerRepository;
    }

    LosBehandlingDtoTjeneste() {
        //for CDI proxy
    }

    public LosBehandlingDto lagLosBehandlingDto(Behandling behandling, boolean harInnhentetRegisterData) {

        return new LosBehandlingDto(behandling.getUuid(),
            Kildesystem.FPSAK,
            behandling.getFagsak().getSaksnummer().getVerdi(),
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
            harInnhentetRegisterData && mapFaresignaler(behandling),
            harRefusjonskrav(behandling),
            lagFagsakEgenskaper(behandling.getFagsak()),
            mapForeldrepengerUttak(behandling),
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
        behandling.getBehandlingÅrsaker().stream()
            .map(BehandlingÅrsak::getBehandlingÅrsakType)
            .map(LosBehandlingDtoTjeneste::mapBehandlingsårsak)
            .filter(behandlingsårsak -> !Behandlingsårsak.ANNET.equals(behandlingsårsak))
            .forEach(årsaker::add);
        if (BehandlingType.REVURDERING.equals(behandling.getType())
            && behandling.getBehandlingÅrsaker().stream().anyMatch(BehandlingÅrsak::erManueltOpprettet)) {
            årsaker.add(Behandlingsårsak.MANUELL);
        }
        return årsaker;
    }

    private static Behandlingsårsak mapBehandlingsårsak(BehandlingÅrsakType årsak) {
        return switch (årsak) {
            case BERØRT_BEHANDLING -> Behandlingsårsak.BERØRT;
            case RE_ENDRING_FRA_BRUKER -> Behandlingsårsak.SØKNAD;
            case RE_VEDTAK_PLEIEPENGER -> Behandlingsårsak.PLEIEPENGER;
            case KLAGE_TILBAKEBETALING -> Behandlingsårsak.KLAGE_TILBAKEBETALING;
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

    private LosBehandlingDto.LosForeldrepengerDto mapForeldrepengerUttak(Behandling behandling) {
        var aggregat = ytelseFordelingTjeneste.hentAggregatHvisEksisterer(behandling.getId());
        var vurderSykdom = aggregat.map(YtelseFordelingAggregat::getGjeldendeFordeling).map(OppgittFordelingEntitet::getPerioder).orElse(List.of())
            .stream().anyMatch(LosBehandlingDtoTjeneste::periodeGjelderSykdom);
        var gradering = aggregat.map(YtelseFordelingAggregat::getGjeldendeFordeling).map(OppgittFordelingEntitet::getPerioder).orElse(List.of())
            .stream().anyMatch(OppgittPeriodeEntitet::isGradert);
        if (FagsakYtelseType.UDEFINERT.equals(behandling.getFagsakYtelseType()) || !behandling.erYtelseBehandling()) {
            return null;
        }
        if (FagsakYtelseType.ENGANGSTØNAD.equals(behandling.getFagsakYtelseType()) || BehandlingType.FØRSTEGANGSSØKNAD.equals(behandling.getType())) {
            var uttakEllerSkjæringstidspunkt = finnUttakEllerUtledetSkjæringstidspunkt(behandling);
            return new LosBehandlingDto.LosForeldrepengerDto(uttakEllerSkjæringstidspunkt, vurderSykdom, gradering);
        }
        var endretUttakFom = FagsakYtelseType.FORELDREPENGER.equals(behandling.getFagsakYtelseType()) ?
            finnEndringsdatoForeldrepenger(behandling, aggregat) : finnEndringsdatoSvangerskapspenger(behandling);
        var endringEllerFørsteUttak = endretUttakFom.orElseGet(() -> finnUttakEllerUtledetSkjæringstidspunkt(behandling));
        return new LosBehandlingDto.LosForeldrepengerDto(endringEllerFørsteUttak, vurderSykdom, gradering);
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

    private Optional<LocalDate> finnEndringsdatoForeldrepenger(Behandling behandling, Optional<YtelseFordelingAggregat> aggregat) {
        var endringsdato = aggregat.flatMap(YtelseFordelingAggregat::getAvklarteDatoer).map(AvklarteUttakDatoerEntitet::getGjeldendeEndringsdato);
        // Andre revurderinger enn endringssøknad har kopiert fordeling fra forrige behandling - kan ikke se på dem.
        return !behandling.harBehandlingÅrsak(BehandlingÅrsakType.RE_ENDRING_FRA_BRUKER)? endringsdato : endringsdato
            .or(() -> aggregat.map(YtelseFordelingAggregat::getGjeldendeFordeling)
                .map(OppgittFordelingEntitet::getPerioder).orElse(List.of()).stream()
                .map(OppgittPeriodeEntitet::getFom)
                .min(Comparator.naturalOrder()));
    }

    private Optional<LocalDate> finnEndringsdatoSvangerskapspenger(Behandling behandling) {
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

    private boolean harRefusjonskrav(Behandling behandling) {
        if (FagsakYtelseType.ENGANGSTØNAD.equals(behandling.getFagsakYtelseType()) || !behandling.erYtelseBehandling() || behandling.harÅpentAksjonspunktMedType(AksjonspunktDefinisjon.VURDER_ARBEIDSFORHOLD_INNTEKTSMELDING)) {
            return false;
        }
        var inntektsmeldinger = inntektsmeldingTjeneste.hentAlleInntektsmeldingerForAngitteBehandlinger(Set.of(behandling.getId()));
        return !inntektsmeldinger.isEmpty() && inntektsmeldinger.stream()
            .map(Inntektsmelding::getRefusjonBeløpPerMnd)
            .filter(Objects::nonNull)
            .allMatch(beløp -> beløp.compareTo(Beløp.ZERO) > 0);
    }

    public LosFagsakEgenskaperDto lagFagsakEgenskaper(Fagsak fagsak) {
        var markering = fagsakEgenskapRepository.finnFagsakMarkering(fagsak.getId()).map(this::mapMarkering).orElse(null);
        return new LosFagsakEgenskaperDto(markering);
    }

    private LosFagsakEgenskaperDto.FagsakMarkering mapMarkering(FagsakMarkering markering) {
        return  switch (markering) {
            case NASJONAL -> LosFagsakEgenskaperDto.FagsakMarkering.NASJONAL;
            case EØS_BOSATT_NORGE -> LosFagsakEgenskaperDto.FagsakMarkering.EØS_BOSATT_NORGE;
            case BOSATT_UTLAND -> LosFagsakEgenskaperDto.FagsakMarkering.BOSATT_UTLAND;
            case SAMMENSATT_KONTROLL -> LosFagsakEgenskaperDto.FagsakMarkering.SAMMENSATT_KONTROLL;
            case DØD_DØDFØDSEL -> LosFagsakEgenskaperDto.FagsakMarkering.DØD;
            case SELVSTENDIG_NÆRING -> LosFagsakEgenskaperDto.FagsakMarkering.NÆRING;
        };
    }


}
