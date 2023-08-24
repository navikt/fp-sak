package no.nav.foreldrepenger.web.app.tjenester.fpoversikt;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.MottattDokument;
import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.SvangerskapspengerRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.SvpTilretteleggingEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.TilretteleggingFOM;
import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.TilretteleggingType;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtak;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.årsak.UtsettelseÅrsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.uttak.UttakArbeidType;
import no.nav.foreldrepenger.behandlingslager.uttak.svp.SvangerskapspengerUttakResultatArbeidsforholdEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.svp.SvangerskapspengerUttakResultatEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.svp.SvangerskapspengerUttakResultatPeriodeEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.svp.SvangerskapspengerUttakResultatRepository;
import no.nav.foreldrepenger.behandlingslager.virksomhet.ArbeidType;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektsmeldingTjeneste;
import no.nav.foreldrepenger.domene.iay.modell.Inntektsmelding;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktTjeneste;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef.nullRef;
import static no.nav.foreldrepenger.web.app.tjenester.fpoversikt.DtoTjenesteFelles.statusForSøknad;

@ApplicationScoped
public class SvpDtoTjeneste {

    private static final String UNEXPECTED_VALUE = "Unexpected value: ";
    private SvangerskapspengerRepository svangerskapspengerRepository;
    private SvangerskapspengerUttakResultatRepository svangerskapspengerUttakResultatRepository;
    private DtoTjenesteFelles felles;
    private InntektsmeldingTjeneste inntektsmeldingTjeneste;
    private SkjæringstidspunktTjeneste skjæringstidspunktTjeneste;

    @Inject
    public SvpDtoTjeneste(SvangerskapspengerRepository svangerskapspengerRepository,
                          SvangerskapspengerUttakResultatRepository svangerskapspengerUttakResultatRepository,
                          DtoTjenesteFelles felles,
                          InntektsmeldingTjeneste inntektsmeldingTjeneste,
                          @FagsakYtelseTypeRef(FagsakYtelseType.SVANGERSKAPSPENGER) SkjæringstidspunktTjeneste skjæringstidspunktTjeneste) {
        this.svangerskapspengerRepository = svangerskapspengerRepository;
        this.svangerskapspengerUttakResultatRepository = svangerskapspengerUttakResultatRepository;
        this.felles = felles;
        this.inntektsmeldingTjeneste = inntektsmeldingTjeneste;
        this.skjæringstidspunktTjeneste = skjæringstidspunktTjeneste;
    }

    SvpDtoTjeneste() {
        //CDI
    }

    public Sak hentSak(Fagsak fagsak) {
        if (fagsak.getYtelseType() != FagsakYtelseType.SVANGERSKAPSPENGER) {
            throw new IllegalArgumentException("Forventer bare svp fagsaker");
        }
        var saksnummer = fagsak.getSaksnummer().getVerdi();
        var aktørId = fagsak.getAktørId().getId();
        var gjeldendeVedtak = felles.finnGjeldendeVedtak(fagsak);
        var åpenYtelseBehandling = felles.hentÅpenBehandling(fagsak);
        var familieHendelse = felles.finnFamilieHendelse(fagsak, gjeldendeVedtak, åpenYtelseBehandling);
        var erSakAvsluttet = felles.erAvsluttet(fagsak);
        var ikkeHenlagteBehandlinger = felles.finnIkkeHenlagteBehandlinger(fagsak);
        var aksjonspunkt = felles.finnAksjonspunkt(ikkeHenlagteBehandlinger);
        var mottatteSøknader = felles.finnRelevanteSøknadsdokumenter(fagsak);
        var alleVedtak = felles.finnVedtakForFagsak(fagsak);
        var søknader = finnSvpSøknader(åpenYtelseBehandling, mottatteSøknader);
        var vedtak = finnSvpVedtak(alleVedtak);
        return new SvpSak(saksnummer, aktørId, familieHendelse, erSakAvsluttet, aksjonspunkt, søknader, vedtak);
    }

    private Set<SvpSak.Vedtak> finnSvpVedtak(Stream<BehandlingVedtak> vedtak) {
        return vedtak.map(v -> {
            var arbeidsforhold = finnArbeidsforhold(v);
            var vedtakstidspunkt = v.getVedtakstidspunkt();
            var avslagÅrsak = finnAvslagÅrsak(v);
            return new SvpSak.Vedtak(vedtakstidspunkt, arbeidsforhold, avslagÅrsak);
        }).collect(Collectors.toSet());
    }

    private SvpSak.Vedtak.AvslagÅrsak finnAvslagÅrsak(BehandlingVedtak vedtak) {
        var avslagsårsak = vedtak.getBehandlingsresultat().getAvslagsårsak();
        if (avslagsårsak == null) {
            return null;
        }
        return switch (avslagsårsak) {
            case ARBEIDSTAKER_KAN_OMPLASSERES, SN_FL_HAR_MULIGHET_TIL_Å_TILRETTELEGGE_SITT_VIRKE -> SvpSak.Vedtak.AvslagÅrsak.ARBEIDSGIVER_KAN_TILRETTELEGGE;
            case SØKER_HAR_MOTTATT_SYKEPENGER -> SvpSak.Vedtak.AvslagÅrsak.SØKER_ER_INNVILGET_SYKEPENGER;
            case MANGLENDE_DOKUMENTASJON -> SvpSak.Vedtak.AvslagÅrsak.MANGLENDE_DOKUMENTASJON;
            default -> SvpSak.Vedtak.AvslagÅrsak.ANNET;
        };
    }

    private Set<SvpSak.Vedtak.ArbeidsforholdUttak> finnArbeidsforhold(BehandlingVedtak vedtak) {
        var behandlingsresultat = vedtak.getBehandlingsresultat();
        var behandlingId = behandlingsresultat.getBehandlingId();
        var uttak = svangerskapspengerUttakResultatRepository.hentHvisEksisterer(behandlingId);
        if (uttak.map(this::bareFinnesInnvilgetPerioder).orElse(false) && behandlingsresultat.isBehandlingsresultatAvslåttOrOpphørt()) {
            return Set.of();
        }

        var behandling = felles.finnBehandling(behandlingId);
        return uttak.map(u -> {
            var tilretteleggingListe = svangerskapspengerRepository.hentGrunnlag(behandlingId)
                .orElseThrow()
                .getGjeldendeVersjon()
                .getTilretteleggingListe();
            var uttaksResultatArbeidsforhold = u.getUttaksResultatArbeidsforhold();
            return uttaksResultatArbeidsforhold.stream().map(ua -> {
                var type = mapTilAktivitetType(ua.getUttakArbeidType());
                var arbeidsgiver = ua.getArbeidsgiver() == null ? null : new Arbeidsgiver(ua.getArbeidsgiver().getIdentifikator());
                var arbeidsforholdId = ua.getArbeidsforholdRef() == null ? null : ua.getArbeidsforholdRef().getReferanse();
                var ikkeOppfyltÅrsak = switch (ua.getArbeidsforholdIkkeOppfyltÅrsak()) {
                    case INGEN -> null;
                    case HELE_UTTAKET_ER_ETTER_3_UKER_FØR_TERMINDATO, UTTAK_KUN_PÅ_HELG ->
                        SvpSak.Vedtak.ArbeidsforholdUttak.ArbeidsforholdIkkeOppfyltÅrsak.ANNET;
                    case ARBEIDSGIVER_KAN_TILRETTELEGGE ->
                        SvpSak.Vedtak.ArbeidsforholdUttak.ArbeidsforholdIkkeOppfyltÅrsak.ARBEIDSGIVER_KAN_TILRETTELEGGE;
                    case ARBEIDSGIVER_KAN_TILRETTELEGGE_FREM_TIL_3_UKER_FØR_TERMIN ->
                        SvpSak.Vedtak.ArbeidsforholdUttak.ArbeidsforholdIkkeOppfyltÅrsak.ARBEIDSGIVER_KAN_TILRETTELEGGE_FREM_TIL_3_UKER_FØR_TERMIN;
                };
                var matchendeTilrettelegging = tilretteleggingListe.stream().filter(tl -> matcher(tl, ua)).findFirst();
                var svpPerioder = ua.getPerioder().stream().map(p -> {
                    var matchendeTilretteleggingFOM = matchendeTilrettelegging.map(SvpTilretteleggingEntitet::getTilretteleggingFOMListe)
                        .orElse(List.of())
                        .stream()
                        .sorted((o1, o2) -> o2.getFomDato().compareTo(o1.getFomDato()))
                        .filter(tfom -> !tfom.getFomDato().isAfter(p.getFom()))
                        .findFirst();
                    if (matchendeTilretteleggingFOM.isEmpty() && !p.getUtbetalingsgrad().harUtbetaling()) {
                        //Uttaksperioder opprettet i fpsak. Ligger innvilget med og uten utbetaling i prod
                        return null;
                    }
                    var tilretteleggingType = matchendeTilretteleggingFOM.map(mt -> mapTilretteleggingType(mt.getType()))
                        .orElse(SvpSak.TilretteleggingType.INGEN);
                    var resultatÅrsak = switch (p.getPeriodeIkkeOppfyltÅrsak()) {
                        case INGEN -> SvpSak.Vedtak.ArbeidsforholdUttak.SvpPeriode.ResultatÅrsak.INNVILGET;
                        case _8304, _8305, _8306 -> SvpSak.Vedtak.ArbeidsforholdUttak.SvpPeriode.ResultatÅrsak.OPPHØR_ANNET;
                        case _8308_SØKT_FOR_SENT -> SvpSak.Vedtak.ArbeidsforholdUttak.SvpPeriode.ResultatÅrsak.AVSLAG_SØKNADSFRIST;
                        case _8309 -> SvpSak.Vedtak.ArbeidsforholdUttak.SvpPeriode.ResultatÅrsak.OPPHØR_FØDSEL;
                        case _8310 -> SvpSak.Vedtak.ArbeidsforholdUttak.SvpPeriode.ResultatÅrsak.OPPHØR_TIDSPERIODE_FØR_TERMIN;
                        case _8314 -> SvpSak.Vedtak.ArbeidsforholdUttak.SvpPeriode.ResultatÅrsak.OPPHØR_OVERGANG_FORELDREPENGER;
                        case _8311, PERIODEN_ER_SAMTIDIG_SOM_SYKEPENGER -> SvpSak.Vedtak.ArbeidsforholdUttak.SvpPeriode.ResultatÅrsak.AVSLAG_ANNET;
                        case _8313 -> SvpSak.Vedtak.ArbeidsforholdUttak.SvpPeriode.ResultatÅrsak.OPPHØR_OPPHOLD_I_YTELSEN;
                        case SVANGERSKAPSVILKÅRET_IKKE_OPPFYLT, OPPTJENINGSVILKÅRET_IKKE_OPPFYLT ->
                            SvpSak.Vedtak.ArbeidsforholdUttak.SvpPeriode.ResultatÅrsak.AVSLAG_INNGANGSVILKÅR;
                    };

                    var arbeidstidprosent =  matchendeTilretteleggingFOM
                        .filter(mt -> TilretteleggingType.DELVIS_TILRETTELEGGING.equals(mt.getType()))
                        .map(TilretteleggingFOM::getStillingsprosent)
                        .orElse(null);
                    return new SvpSak.Vedtak.ArbeidsforholdUttak.SvpPeriode(p.getFom(), p.getTom(), tilretteleggingType, arbeidstidprosent,
                        p.getUtbetalingsgrad().decimalValue(), resultatÅrsak);
                }).filter(Objects::nonNull).collect(Collectors.toSet());
                //utlede oppholdsperioder
                var oppholdsperioder = matchendeTilrettelegging.map(mt -> finnAlleOppholdsperioderFraTlr(mt, behandling)).orElse(Set.of());

                return new SvpSak.Vedtak.ArbeidsforholdUttak(new SvpSak.Aktivitet(type, arbeidsgiver, arbeidsforholdId),
                    matchendeTilrettelegging.map(SvpTilretteleggingEntitet::getBehovForTilretteleggingFom).orElse(null),
                    matchendeTilrettelegging.flatMap(SvpTilretteleggingEntitet::getOpplysningerOmRisikofaktorer).orElse(null),
                    matchendeTilrettelegging.flatMap(SvpTilretteleggingEntitet::getOpplysningerOmTilretteleggingstiltak).orElse(null), svpPerioder,
                    oppholdsperioder, ikkeOppfyltÅrsak);
            }).collect(Collectors.toSet());
        }).orElse(Set.of());
    }

    private boolean bareFinnesInnvilgetPerioder(SvangerskapspengerUttakResultatEntitet uttak) {
        var perioder = uttak.getUttaksResultatArbeidsforhold()
            .stream()
            .flatMap(ua -> ua.getPerioder().stream())
            .toList();
        if (perioder.isEmpty()) {
            return false;
        }
        return perioder.stream()
            .allMatch(SvangerskapspengerUttakResultatPeriodeEntitet::isInnvilget);
    }

    private Set<SvpSak.OppholdPeriode> finnAlleOppholdsperioderFraTlr(SvpTilretteleggingEntitet tilrettelegging, Behandling behandling) {
        var oppholdspFraSaksbehandler = oppholdsperioderRegAvSaksbehandler(tilrettelegging);
        Set<SvpSak.OppholdPeriode> alleOppholdForArbforhold = new HashSet<>(oppholdspFraSaksbehandler);
        //opphold fra inntektsmelding
        tilrettelegging.getArbeidsgiver().ifPresent( arbeidsgiver -> {
            var oppholdFraIM = oppholdsperioderFraIM(behandling, arbeidsgiver, tilrettelegging.getInternArbeidsforholdRef().orElse(null));
            alleOppholdForArbforhold.addAll(oppholdFraIM);
            });

        return alleOppholdForArbforhold;
    }

    private static Set<SvpSak.OppholdPeriode> oppholdsperioderRegAvSaksbehandler(SvpTilretteleggingEntitet matchendeTilrettelegging) {
        return matchendeTilrettelegging.getAvklarteOpphold()
            .stream()
            .map(o -> new SvpSak.OppholdPeriode(o.getFom(), o.getTom(), switch (o.getOppholdÅrsak()) {
                case SYKEPENGER -> SvpSak.OppholdPeriode.Årsak.SYKEPENGER;
                case FERIE -> SvpSak.OppholdPeriode.Årsak.FERIE;
            }, SvpSak.OppholdPeriode.OppholdKilde.SAKSBEHANDLER))
            .collect(Collectors.toSet());
    }

    private Set<SvpSak.OppholdPeriode> oppholdsperioderFraIM(Behandling behandling, no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver arbeidsgiver, InternArbeidsforholdRef internArbeidsforholdRef) {
        Set<SvpSak.OppholdPeriode> oppholdFraImForArbeidsgiver = new HashSet<>();
        var inntektsmeldingForArbeidsforhold = finnIMForArbforhold(behandling, arbeidsgiver, internArbeidsforholdRef);

        inntektsmeldingForArbeidsforhold.ifPresent(inntektsmelding -> oppholdFraImForArbeidsgiver.addAll(hentOppholdFraIm(inntektsmelding)));

        return oppholdFraImForArbeidsgiver;
    }

    private Set<SvpSak.OppholdPeriode> hentOppholdFraIm(Inntektsmelding inntektsmelding) {
        return inntektsmelding.getUtsettelsePerioder().stream()
            .filter(utsettelse -> UtsettelseÅrsak.FERIE.equals(utsettelse.getÅrsak()))
            .map(utsettelse -> new SvpSak.OppholdPeriode(utsettelse.getPeriode().getFomDato(), utsettelse.getPeriode().getTomDato(),
                SvpSak.OppholdPeriode.Årsak.FERIE,
                SvpSak.OppholdPeriode.OppholdKilde.INNTEKTSMELDING))
            .collect(Collectors.toSet());
    }

    private Optional<Inntektsmelding> finnIMForArbforhold(Behandling behandling, no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver arbeidsgiver, InternArbeidsforholdRef internArbeidsforholdRef) {
        var skjæringstidspunkter = skjæringstidspunktTjeneste.getSkjæringstidspunkter(behandling.getId());
        var ref = BehandlingReferanse.fra(behandling);
        return inntektsmeldingTjeneste.hentInntektsmeldinger(ref, skjæringstidspunkter.getSkjæringstidspunktOpptjening()).stream()
            .filter(inntektsmelding -> inntektsmelding.getArbeidsgiver().equals(arbeidsgiver) && (internArbeidsforholdRef == null || inntektsmelding.getArbeidsforholdRef().gjelderFor(internArbeidsforholdRef)))
            .findFirst();
    }

    private static SvpSak.Aktivitet.Type mapTilAktivitetType(UttakArbeidType uttakArbeidType) {
        return switch (uttakArbeidType) {
            case ORDINÆRT_ARBEID -> SvpSak.Aktivitet.Type.ORDINÆRT_ARBEID;
            case SELVSTENDIG_NÆRINGSDRIVENDE -> SvpSak.Aktivitet.Type.SELVSTENDIG_NÆRINGSDRIVENDE;
            case FRILANS -> SvpSak.Aktivitet.Type.FRILANS;
            case ANNET -> throw new IllegalStateException(UNEXPECTED_VALUE + uttakArbeidType);
        };
    }

    private boolean matcher(SvpTilretteleggingEntitet tl, SvangerskapspengerUttakResultatArbeidsforholdEntitet arbeidsforhold) {
        var aktivitetType1 = mapTilAktivitetType(tl.getArbeidType());
        var aktivitetType2 = mapTilAktivitetType(arbeidsforhold.getUttakArbeidType());

        return aktivitetType1 == aktivitetType2 && Objects.equals(arbeidsforhold.getArbeidsgiver(), tl.getArbeidsgiver().orElse(null))
            && Objects.equals(arbeidsforhold.getArbeidsforholdRef(), tl.getInternArbeidsforholdRef().orElse(nullRef()));
    }

    private Set<SvpSak.Søknad> finnSvpSøknader(Optional<Behandling> åpenYtelseBehandling, List<MottattDokument> mottatteSøknader) {
        return mottatteSøknader.stream().map(md -> {
            var behandlingId = md.getBehandlingId();
            var status = statusForSøknad(åpenYtelseBehandling, behandlingId);
            var tilrettelegginger = finnTilrettelegginger(behandlingId);
            return new SvpSak.Søknad(status, md.getMottattTidspunkt(), tilrettelegginger);
        }).collect(Collectors.toSet());
    }

    private Set<SvpSak.Søknad.Tilrettelegging> finnTilrettelegginger(Long behandlingId) {
        return svangerskapspengerRepository.hentGrunnlag(behandlingId)
            .map(svpGrunnlag -> svpGrunnlag.getOpprinneligeTilrettelegginger()
                .getTilretteleggingListe()
                .stream()
                .map(SvpDtoTjeneste::map)
                .collect(Collectors.toSet()))
            .orElse(Set.of());
    }

    private static SvpSak.Søknad.Tilrettelegging map(SvpTilretteleggingEntitet tl) {
        var aktivitet = utledAktivitet(tl);
        var perioder = tl.getTilretteleggingFOMListe().stream().map(tFom -> {
            SvpSak.TilretteleggingType tilretteleggingType = mapTilretteleggingType(tFom.getType());
            return new SvpSak.Søknad.Tilrettelegging.Periode(tFom.getFomDato(), tilretteleggingType, tFom.getStillingsprosent());
        }).collect(Collectors.toSet());
        Set<SvpSak.OppholdPeriode> oppholdsperioder = oppholdsperioderRegAvSaksbehandler(tl);
        return new SvpSak.Søknad.Tilrettelegging(aktivitet, tl.getBehovForTilretteleggingFom(), tl.getOpplysningerOmRisikofaktorer().orElse(null),
            tl.getOpplysningerOmTilretteleggingstiltak().orElse(null), perioder, oppholdsperioder);
    }

    private static SvpSak.TilretteleggingType mapTilretteleggingType(TilretteleggingType type) {
        return switch (type) {
            case HEL_TILRETTELEGGING -> SvpSak.TilretteleggingType.HEL;
            case DELVIS_TILRETTELEGGING -> SvpSak.TilretteleggingType.DELVIS;
            case INGEN_TILRETTELEGGING -> SvpSak.TilretteleggingType.INGEN;
        };
    }

    private static SvpSak.Aktivitet utledAktivitet(SvpTilretteleggingEntitet tl) {
        var aktivitetType = mapTilAktivitetType(tl.getArbeidType());
        var arbeidsgiver = tl.getArbeidsgiver().map(a -> new Arbeidsgiver(a.getIdentifikator())).orElse(null);
        return new SvpSak.Aktivitet(aktivitetType, arbeidsgiver, null);
    }

    private static SvpSak.Aktivitet.Type mapTilAktivitetType(ArbeidType arbeidType) {
        return switch (arbeidType) {
            case FRILANSER -> SvpSak.Aktivitet.Type.FRILANS;
            case ORDINÆRT_ARBEIDSFORHOLD -> SvpSak.Aktivitet.Type.ORDINÆRT_ARBEID;
            case SELVSTENDIG_NÆRINGSDRIVENDE -> SvpSak.Aktivitet.Type.SELVSTENDIG_NÆRINGSDRIVENDE;
            default -> throw new IllegalStateException(UNEXPECTED_VALUE + arbeidType);
        };
    }
}
