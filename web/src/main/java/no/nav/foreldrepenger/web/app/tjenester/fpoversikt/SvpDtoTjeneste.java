package no.nav.foreldrepenger.web.app.tjenester.fpoversikt;

import static no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef.nullRef;
import static no.nav.foreldrepenger.web.app.tjenester.fpoversikt.DtoTjenesteFelles.statusForSøknad;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.MottattDokument;
import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.SvangerskapspengerRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.SvpTilretteleggingEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.TilretteleggingType;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtak;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.uttak.UttakArbeidType;
import no.nav.foreldrepenger.behandlingslager.uttak.svp.SvangerskapspengerUttakResultatArbeidsforholdEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.svp.SvangerskapspengerUttakResultatRepository;
import no.nav.foreldrepenger.behandlingslager.virksomhet.ArbeidType;
import no.nav.foreldrepenger.konfig.Environment;

@ApplicationScoped
public class SvpDtoTjeneste {
    private static final Environment ENV = Environment.current();

    private static final String UNEXPECTED_VALUE = "Unexpected value: ";
    private SvangerskapspengerRepository svangerskapspengerRepository;
    private SvangerskapspengerUttakResultatRepository svangerskapspengerUttakResultatRepository;
    private DtoTjenesteFelles felles;

    @Inject
    public SvpDtoTjeneste(SvangerskapspengerRepository svangerskapspengerRepository,
                          SvangerskapspengerUttakResultatRepository svangerskapspengerUttakResultatRepository,
                          DtoTjenesteFelles felles) {
        this.svangerskapspengerRepository = svangerskapspengerRepository;
        this.svangerskapspengerUttakResultatRepository = svangerskapspengerUttakResultatRepository;
        this.felles = felles;
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
        //TODO Finnnes 12 saker i prod med br resultat opphør, men alle uttaksperioder er innvilget. Må håndtere disse
        return vedtak.map(v -> new SvpSak.Vedtak(v.getVedtakstidspunkt(), finnArbeidsforhold(v))).collect(Collectors.toSet());
    }

    private Set<SvpSak.Vedtak.ArbeidsforholdUttak> finnArbeidsforhold(BehandlingVedtak vedtak) {
        if (isProd()) {
            return Set.of();
        }
        var behandlingId = vedtak.getBehandlingsresultat().getBehandlingId();
        return svangerskapspengerUttakResultatRepository.hentHvisEksisterer(behandlingId).map(uttak -> {
            var tilretteleggingListe = svangerskapspengerRepository.hentGrunnlag(behandlingId)
                .orElseThrow()
                .getGjeldendeVersjon()
                .getTilretteleggingListe();
            var uttaksResultatArbeidsforhold = uttak.getUttaksResultatArbeidsforhold();
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
                    var matchendeTilretteleggingFOM = matchendeTilrettelegging.map(mt -> mt.getTilretteleggingFOMListe())
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
                    var arbeidstidprosent = matchendeTilretteleggingFOM.map(mt -> mt.getStillingsprosent()).orElse(BigDecimal.ZERO);
                    return new SvpSak.Vedtak.ArbeidsforholdUttak.SvpPeriode(p.getFom(), p.getTom(), tilretteleggingType, arbeidstidprosent,
                        p.getUtbetalingsgrad().decimalValue(), resultatÅrsak);
                }).filter(Objects::nonNull).collect(Collectors.toSet());
                var oppholdsperioder = matchendeTilrettelegging.map(mt -> oppholdsperioderFraTilrettelegging(mt)).orElse(Set.of());
                return new SvpSak.Vedtak.ArbeidsforholdUttak(new SvpSak.Aktivitet(type, arbeidsgiver, arbeidsforholdId),
                    matchendeTilrettelegging.map(mt -> mt.getBehovForTilretteleggingFom()).orElse(null),
                    matchendeTilrettelegging.flatMap(SvpTilretteleggingEntitet::getOpplysningerOmRisikofaktorer).orElse(null),
                    matchendeTilrettelegging.flatMap(SvpTilretteleggingEntitet::getOpplysningerOmTilretteleggingstiltak).orElse(null), svpPerioder,
                    oppholdsperioder, ikkeOppfyltÅrsak);
            }).collect(Collectors.toSet());
        }).orElse(Set.of());
    }

    private static Set<SvpSak.OppholdPeriode> oppholdsperioderFraTilrettelegging(SvpTilretteleggingEntitet matchendeTilrettelegging) {
        return matchendeTilrettelegging.getAvklarteOpphold()
            .stream()
            .map(o -> new SvpSak.OppholdPeriode(o.getFom(), o.getTom(), switch (o.getOppholdÅrsak()) {
                case SYKEPENGER -> SvpSak.OppholdPeriode.Årsak.SYKEPENGER;
                case FERIE -> SvpSak.OppholdPeriode.Årsak.FERIE;
            }))
            .collect(Collectors.toSet());
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
        if (isProd()) {
            return Set.of();
        }
        return svangerskapspengerRepository.hentGrunnlag(behandlingId)
            .map(svpGrunnlag -> svpGrunnlag.getOpprinneligeTilrettelegginger()
                .getTilretteleggingListe()
                .stream()
                .map(tl -> map(tl))
                .collect(Collectors.toSet()))
            .orElse(Set.of());
    }

    private static boolean isProd() {
        return ENV.isProd();
    }

    private static SvpSak.Søknad.Tilrettelegging map(SvpTilretteleggingEntitet tl) {
        var aktivitet = utledAktivitet(tl);
        var perioder = tl.getTilretteleggingFOMListe().stream().map(tFom -> {
            SvpSak.TilretteleggingType tilretteleggingType = mapTilretteleggingType(tFom.getType());
            return new SvpSak.Søknad.Tilrettelegging.Periode(tFom.getFomDato(), tilretteleggingType, tFom.getStillingsprosent());
        }).collect(Collectors.toSet());
        Set<SvpSak.OppholdPeriode> oppholdsperioder = oppholdsperioderFraTilrettelegging(tl);
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
