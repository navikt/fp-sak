package no.nav.foreldrepenger.web.app.tjenester.behandling.svp;

import static no.nav.foreldrepenger.domene.arbeidInntektsmelding.HåndterePermisjoner.finnRelevantePermisjonSomOverlapperTilretteleggingFom;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.SvangerskapspengerRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.SvpGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.SvpOppholdÅrsak;
import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.SvpTilretteleggingEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.SvpTilretteleggingFomKilde;
import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.SvpTilretteleggingerEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.TilretteleggingFOM;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.årsak.UtsettelseÅrsak;
import no.nav.foreldrepenger.behandlingslager.uttak.UttakArbeidType;
import no.nav.foreldrepenger.behandlingslager.virksomhet.ArbeidType;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektsmeldingTjeneste;
import no.nav.foreldrepenger.domene.iay.modell.AktivitetsAvtale;
import no.nav.foreldrepenger.domene.iay.modell.AktørArbeid;
import no.nav.foreldrepenger.domene.iay.modell.ArbeidsforholdInformasjon;
import no.nav.foreldrepenger.domene.iay.modell.ArbeidsforholdOverstyring;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.foreldrepenger.domene.iay.modell.Inntektsmelding;
import no.nav.foreldrepenger.domene.iay.modell.Permisjon;
import no.nav.foreldrepenger.domene.iay.modell.Yrkesaktivitet;
import no.nav.foreldrepenger.domene.iay.modell.YrkesaktivitetFilter;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.BekreftetPermisjonStatus;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;
import no.nav.foreldrepenger.domene.typer.Stillingsprosent;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktTjeneste;
import no.nav.vedtak.konfig.Tid;

@ApplicationScoped
public class SvangerskapspengerTjeneste {
    private static final Logger LOG = LoggerFactory.getLogger(SvangerskapspengerTjeneste.class);

    private static final Map<ArbeidType, UttakArbeidType> ARBTYPE_MAP = Map.ofEntries(
        Map.entry(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD, UttakArbeidType.ORDINÆRT_ARBEID), Map.entry(ArbeidType.FRILANSER, UttakArbeidType.FRILANS),
        Map.entry(ArbeidType.SELVSTENDIG_NÆRINGSDRIVENDE, UttakArbeidType.SELVSTENDIG_NÆRINGSDRIVENDE));

    private SvangerskapspengerRepository svangerskapspengerRepository;
    private FamilieHendelseRepository familieHendelseRepository;
    private InntektArbeidYtelseTjeneste iayTjeneste;
    private InntektsmeldingTjeneste inntektsmeldingTjeneste;
    private SkjæringstidspunktTjeneste skjæringstidspunktTjeneste;

    public SvangerskapspengerTjeneste() {
        //CDI greier
    }

    @Inject
    public SvangerskapspengerTjeneste(SvangerskapspengerRepository svangerskapspengerRepository,
                                      FamilieHendelseRepository familieHendelseRepository,
                                      InntektArbeidYtelseTjeneste iayTjeneste,
                                      InntektsmeldingTjeneste inntektsmeldingTjeneste,
                                      SkjæringstidspunktTjeneste skjæringstidspunktTjeneste) {
        this.svangerskapspengerRepository = svangerskapspengerRepository;
        this.familieHendelseRepository = familieHendelseRepository;
        this.iayTjeneste = iayTjeneste;
        this.inntektsmeldingTjeneste = inntektsmeldingTjeneste;
        this.skjæringstidspunktTjeneste = skjæringstidspunktTjeneste;
    }

    public SvpTilretteleggingDto hentTilrettelegging(Behandling behandling) {
        var behandlingId = behandling.getId();
        var svpTilretteleggingDto = new SvpTilretteleggingDto();

        var familieHendelseGrunnlag = familieHendelseRepository.hentAggregatHvisEksisterer(behandlingId);
        if (familieHendelseGrunnlag.isEmpty()) {
            return svpTilretteleggingDto;
        }
        var terminbekreftelse = familieHendelseGrunnlag.get().getGjeldendeTerminbekreftelse();
        if (terminbekreftelse.isEmpty()) {
            throw SvangerskapsTjenesteFeil.kanIkkeFinneTerminbekreftelsePåSvangerskapspengerSøknad(behandlingId);
        }
        svpTilretteleggingDto.setTermindato(terminbekreftelse.get().getTermindato());
        familieHendelseGrunnlag.get().getGjeldendeVersjon().getFødselsdato().ifPresent(svpTilretteleggingDto::setFødselsdato);
        svpTilretteleggingDto.setSaksbehandlet(harSaksbehandletTilrettelegging(behandling));

        //Hent informasjon for å mappe arbeidsforholdDto
        var aktørId = behandling.getAktørId();
        var gjeldendeTilrettelegginger = hentGjeldendeTilrettelegginger(behandlingId);
        var opprinneligeTilrettelegginger = hentOpprinneligeTilrettlegginger(behandlingId);
        var iayGrunnlag = iayTjeneste.hentGrunnlag(behandlingId);
        var arbeidsforholdInformasjon = iayGrunnlag.getArbeidsforholdInformasjon()
            .orElseThrow(
                () -> new IllegalStateException("Utviklerfeil: Fant ikke forventent arbeidsforholdinformasjon for behandling: " + behandlingId));
        var registerFilter = new YrkesaktivitetFilter(iayGrunnlag.getArbeidsforholdInformasjon(), iayGrunnlag.getAktørArbeidFraRegister(aktørId));
        var saksbehandletFilter = new YrkesaktivitetFilter(iayGrunnlag.getArbeidsforholdInformasjon(),
            finnSaksbehandletHvisEksisterer(aktørId, iayGrunnlag));
        var stp = skjæringstidspunktTjeneste.getSkjæringstidspunkter(behandlingId);
        var inntektsmeldinger = inntektsmeldingTjeneste.hentInntektsmeldinger(BehandlingReferanse.fra(behandling),
                stp.getSkjæringstidspunktOpptjening());

        gjeldendeTilrettelegginger.forEach(tilr -> {
            var arbeidsforholdDto = mapArbeidsforholdDto(tilr, behandling, opprinneligeTilrettelegginger, inntektsmeldinger, registerFilter,
                saksbehandletFilter, arbeidsforholdInformasjon);
            svpTilretteleggingDto.leggTilArbeidsforhold(arbeidsforholdDto);
        });
        return svpTilretteleggingDto;
    }

    private List<SvpTilretteleggingEntitet> hentOpprinneligeTilrettlegginger(Long behandlingId) {
        return svangerskapspengerRepository.hentGrunnlag(behandlingId)
            .map(SvpGrunnlagEntitet::getOpprinneligeTilrettelegginger)
            .map(SvpTilretteleggingerEntitet::getTilretteleggingListe)
            .orElse(Collections.emptyList());
    }

    private List<SvpTilretteleggingEntitet> hentGjeldendeTilrettelegginger(Long behandlingId) {
        return svangerskapspengerRepository.hentGrunnlag(behandlingId)
            .map(SvpGrunnlagEntitet::getGjeldendeVersjon)
            .map(SvpTilretteleggingerEntitet::getTilretteleggingListe)
            .orElseThrow(() -> SvangerskapsTjenesteFeil.kanIkkeFinneSvangerskapspengerGrunnlagForBehandling(behandlingId));
    }

    public Optional<BigDecimal> utledStillingsprosentForTilrPeriode(YrkesaktivitetFilter registerFilter, SvpTilretteleggingEntitet tilrettelegging) {
        if (ArbeidType.ORDINÆRT_ARBEIDSFORHOLD.equals(tilrettelegging.getArbeidType())) {
            var førsteTilrStartDato = tilrettelegging.getTilretteleggingFOMListe()
                .stream()
                .map(TilretteleggingFOM::getFomDato)
                .min(Comparator.naturalOrder())
                .orElse(null);

            var yrkesaktivitet = registerFilter.getYrkesaktiviteter()
                .stream()
                .filter(ya -> Objects.equals(ya.getArbeidsgiver(), tilrettelegging.getArbeidsgiver().orElse(null))
                    && tilrettelegging.getInternArbeidsforholdRef().orElse(InternArbeidsforholdRef.nullRef()).gjelderFor(ya.getArbeidsforholdRef()))
                .toList();

            if (yrkesaktivitet.isEmpty() || førsteTilrStartDato == null) {
                return Optional.of(BigDecimal.ZERO);
            }
            var stillingsprosent =finnStillingsprosentForDato(yrkesaktivitet, førsteTilrStartDato);
            if (stillingsprosent.compareTo(BigDecimal.valueOf(100)) > 0) {
                var arbeidsgiverident = tilrettelegging.getArbeidsgiver().stream().map(Arbeidsgiver::getIdentifikator);
                LOG.info("SvangerskapspengerTjeneste: utledStillingsprosentForTilrPeriode: Stillingsprosent over 100% for tilrettelegging med id:{} og arbeidsgviver:{}, stillingsprosent:{}", tilrettelegging.getId(), arbeidsgiverident, stillingsprosent);
            }
            return Optional.of(stillingsprosent);
        } else {
            return Optional.empty();
        }
    }

    private BigDecimal finnStillingsprosentForDato(List<Yrkesaktivitet> yrkesaktiviteter, LocalDate førsteTilrStartDato) {
        return yrkesaktiviteter.stream()
            .map( ya -> hentStillingsprosentForAktivitet(ya, førsteTilrStartDato))
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal hentStillingsprosentForAktivitet(Yrkesaktivitet ya, LocalDate førsteTilrStartDato) {
        //Dersom ingen periode overlapper er stillingsprosent 0
        return  ya.getAlleAktivitetsAvtaler().stream()
            .filter(aa -> !aa.erAnsettelsesPeriode() && aa.getPeriode().inkluderer(førsteTilrStartDato))
            .filter(aa -> aa.getProsentsats() != null && aa.getProsentsats().getVerdi() != null)
            .max(Comparator.comparing(AktivitetsAvtale::getPeriode))
            .map(AktivitetsAvtale::getProsentsats)
            .map(Stillingsprosent::getVerdi)
            .orElse(BigDecimal.ZERO);
    }

    private Optional<AktørArbeid> finnSaksbehandletHvisEksisterer(AktørId aktørId, InntektArbeidYtelseGrunnlag g) {
        if (g.harBlittSaksbehandlet()) {
            return g.getSaksbehandletVersjon()
                .flatMap(aggregat -> aggregat.getAktørArbeid().stream().filter(aa -> aa.getAktørId().equals(aktørId)).findFirst());
        }
        return Optional.empty();
    }

    private boolean erTilgjengeligForBeregning(SvpTilretteleggingEntitet tilr, YrkesaktivitetFilter filter) {
        if (tilr.getArbeidsgiver().isEmpty()) {
            return true;
        }
        if (filter.getYrkesaktiviteterForBeregning().isEmpty()) {
            return false;
        }
        return filter.getYrkesaktiviteterForBeregning()
            .stream()
            .anyMatch(ya -> Objects.equals(ya.getArbeidsgiver(), tilr.getArbeidsgiver().orElse(null)) && tilr.getInternArbeidsforholdRef()
                .orElse(InternArbeidsforholdRef.nullRef())
                .gjelderFor(ya.getArbeidsforholdRef()));
    }

    /**
     * Må se på aksjonspunkt ettersom gjeldende tilrettelegginger ikke bare brukes av saksbehandler
     */
    private boolean harSaksbehandletTilrettelegging(Behandling behandling) {
        var aksjonspunkt = behandling.getAksjonspunktMedDefinisjonOptional(AksjonspunktDefinisjon.VURDER_SVP_TILRETTELEGGING);
        return aksjonspunkt.isPresent() && aksjonspunkt.get().erUtført();
    }

    private SvpArbeidsforholdDto mapArbeidsforholdDto(SvpTilretteleggingEntitet svpTilrettelegging,
                                                      Behandling behandling,
                                                      List<SvpTilretteleggingEntitet> opprinneligeTilrettelegginger,
                                                      List<Inntektsmelding> inntektsmeldinger,
                                                      YrkesaktivitetFilter registerFilter,
                                                      YrkesaktivitetFilter saksbehandletFilter,
                                                      ArbeidsforholdInformasjon arbeidsforholdInformasjon) {

        var arbeidsforholdDto = new SvpArbeidsforholdDto();
        arbeidsforholdDto.setTilretteleggingId(svpTilrettelegging.getId());
        arbeidsforholdDto.setTilretteleggingBehovFom(svpTilrettelegging.getBehovForTilretteleggingFom());
        arbeidsforholdDto.setTilretteleggingDatoer(utledTilretteleggingDatoer(svpTilrettelegging, opprinneligeTilrettelegginger, behandling));
        arbeidsforholdDto.setAvklarteOppholdPerioder(mapAvklartOppholdPeriode(svpTilrettelegging));
        // Ferie fra inntektsmelding skal vises til saksbehandler hvis finnes
        svpTilrettelegging.getArbeidsgiver()
            .flatMap(arbeidsgiver -> finnIMForArbeidsforhold(inntektsmeldinger, arbeidsgiver,
                svpTilrettelegging.getInternArbeidsforholdRef().orElse(InternArbeidsforholdRef.nullRef())))
            .ifPresent(im -> arbeidsforholdDto.leggTilAvklarteOppholdPerioder(hentFerieFraIM(im)));
        arbeidsforholdDto.setOpplysningerOmRisiko(svpTilrettelegging.getOpplysningerOmRisikofaktorer().orElse(null));
        arbeidsforholdDto.setOpplysningerOmTilrettelegging(svpTilrettelegging.getOpplysningerOmTilretteleggingstiltak().orElse(null));
        arbeidsforholdDto.setBegrunnelse(svpTilrettelegging.getBegrunnelse().orElse(null));
        arbeidsforholdDto.setKopiertFraTidligereBehandling(svpTilrettelegging.getKopiertFraTidligereBehandling());
        arbeidsforholdDto.setMottattTidspunkt(svpTilrettelegging.getMottattTidspunkt());
        arbeidsforholdDto.setSkalBrukes(svpTilrettelegging.getSkalBrukes());
        arbeidsforholdDto.setUttakArbeidType(ARBTYPE_MAP.getOrDefault(svpTilrettelegging.getArbeidType(), UttakArbeidType.ANNET));
        svpTilrettelegging.getArbeidsgiver().ifPresent(ag -> arbeidsforholdDto.setArbeidsgiverReferanse(ag.getIdentifikator()));
        svpTilrettelegging.getInternArbeidsforholdRef().ifPresent(ref -> arbeidsforholdDto.setInternArbeidsforholdReferanse(ref.getReferanse()));
        utledStillingsprosentForTilrPeriode(registerFilter, svpTilrettelegging).ifPresent(arbeidsforholdDto::setStillingsprosentStartTilrettelegging);
        arbeidsforholdDto.setVelferdspermisjoner(finnRelevanteVelferdspermisjoner(svpTilrettelegging, registerFilter, saksbehandletFilter));
        finnEksternRef(svpTilrettelegging, arbeidsforholdInformasjon).ifPresent(arbeidsforholdDto::setEksternArbeidsforholdReferanse);
        arbeidsforholdDto.setKanTilrettelegges(erTilgjengeligForBeregning(svpTilrettelegging, registerFilter));
        return arbeidsforholdDto;
    }

    private Optional<Inntektsmelding> finnIMForArbeidsforhold(List<Inntektsmelding> inntektsmeldinger,
                                                              Arbeidsgiver arbeidsgiver,
                                                              InternArbeidsforholdRef internArbeidsforholdRef) {
        return inntektsmeldinger.stream()
            .filter(im -> im.getArbeidsgiver().equals(arbeidsgiver) && im.getArbeidsforholdRef().gjelderFor(internArbeidsforholdRef))
            .findFirst();
    }

    private Optional<String> finnEksternRef(SvpTilretteleggingEntitet svpTilrettelegging, ArbeidsforholdInformasjon arbeidsforholdInformasjon) {
        return svpTilrettelegging.getInternArbeidsforholdRef().map(ref -> {
            var arbeidsgiver = svpTilrettelegging.getArbeidsgiver()
                .orElseThrow(() -> new IllegalStateException(
                    "Utviklerfeil: Fant ikke forventent arbeidsgiver for tilrettelegging: " + svpTilrettelegging.getId()));
            return arbeidsforholdInformasjon.finnEkstern(arbeidsgiver, ref).getReferanse();
        });
    }

    private List<VelferdspermisjonDto> finnRelevanteVelferdspermisjoner(SvpTilretteleggingEntitet svpTilrettelegging,
                                                                        YrkesaktivitetFilter registerFilter,
                                                                        YrkesaktivitetFilter saksbehandletFilter) {
        return svpTilrettelegging.getArbeidsgiver()
            .map(a -> mapVelferdspermisjoner(svpTilrettelegging, registerFilter, a, saksbehandletFilter))
            .orElse(Collections.emptyList());
    }

    private List<VelferdspermisjonDto> mapVelferdspermisjoner(SvpTilretteleggingEntitet svpTilrettelegging,
                                                              YrkesaktivitetFilter registerFilter,
                                                              Arbeidsgiver arbeidsgiver,
                                                              YrkesaktivitetFilter saksbehandletFilter) {
        return registerFilter.getYrkesaktiviteter()
            .stream()
            .filter(ya -> erSammeArbeidsgiver(ya, arbeidsgiver, svpTilrettelegging))
            .flatMap(ya -> finnRelevantePermisjonSomOverlapperTilretteleggingFom(ya, svpTilrettelegging.getBehovForTilretteleggingFom()).stream())
            .map(p -> mapPermisjon(p, registerFilter, saksbehandletFilter))
            .toList();
    }

    private boolean erSammeArbeidsgiver(Yrkesaktivitet yrkesaktivitet, Arbeidsgiver arbeidsgiver, SvpTilretteleggingEntitet svpTilrettelegging) {
        return yrkesaktivitet.getArbeidsgiver() != null && yrkesaktivitet.getArbeidsgiver().getIdentifikator().equals(arbeidsgiver.getIdentifikator())
            && svpTilrettelegging.getInternArbeidsforholdRef()
            .orElse(InternArbeidsforholdRef.nullRef())
            .gjelderFor(yrkesaktivitet.getArbeidsforholdRef());
    }

    private VelferdspermisjonDto mapPermisjon(Permisjon p, YrkesaktivitetFilter registerFilter, YrkesaktivitetFilter saksbehandletFilter) {
        return new VelferdspermisjonDto(p.getFraOgMed(),
            p.getTilOgMed() == null || p.getTilOgMed().isEqual(Tid.TIDENES_ENDE) ? null : p.getTilOgMed(), p.getProsentsats().getVerdi(),
            p.getPermisjonsbeskrivelseType(), erGyldig(p, registerFilter, saksbehandletFilter));
    }

    private Boolean erGyldig(Permisjon p, YrkesaktivitetFilter yrkesfilter, YrkesaktivitetFilter saksbehandletFilter) {
        var arbeidsgiver = p.getYrkesaktivitet().getArbeidsgiver();
        var arbeidsforholdRef = p.getYrkesaktivitet().getArbeidsforholdRef();
        var saksbehandletAktivitet = saksbehandletFilter.getYrkesaktiviteter()
            .stream()
            .filter(ya -> ya.getArbeidsgiver() != null && ya.getArbeidsgiver().getIdentifikator().equals(arbeidsgiver.getIdentifikator())
                && ya.getArbeidsforholdRef().gjelderFor(arbeidsforholdRef))
            .findFirst();
        if (saksbehandletAktivitet.isPresent()) {
            // I svangerskapspenger ble permisjonsvalg før lagret på saksbehandlet versjon. Dette er nå endret til å lagres på arbeidsforholdinformasjon
            var saksbehandletPermisjon = saksbehandletAktivitet.get().getPermisjon();
            return saksbehandletPermisjon.stream()
                .anyMatch(
                    sp -> sp.getPermisjonsbeskrivelseType().equals(p.getPermisjonsbeskrivelseType()) && sp.getFraOgMed().isEqual(p.getFraOgMed())
                        && sp.getProsentsats().getVerdi().compareTo(p.getProsentsats().getVerdi()) == 0);
        } else {
            var bekreftetPermisjonValg = yrkesfilter.getArbeidsforholdOverstyringer()
                .stream()
                .filter(os -> os.getArbeidsgiver().equals(arbeidsgiver) && os.getArbeidsforholdRef().gjelderFor(arbeidsforholdRef))
                .findFirst()
                .flatMap(ArbeidsforholdOverstyring::getBekreftetPermisjon);
            return bekreftetPermisjonValg.map(os -> os.getStatus().equals(BekreftetPermisjonStatus.BRUK_PERMISJON)).orElse(null);
        }
    }

    private List<SvpTilretteleggingDatoDto> utledTilretteleggingDatoer(SvpTilretteleggingEntitet svpTilrettelegging,
                                                                       List<SvpTilretteleggingEntitet> opprinneligeTilr,
                                                                       Behandling behandling) {
        List<SvpTilretteleggingDatoDto> tilretteleggingDatoDtos = new ArrayList<>();
        svpTilrettelegging.getTilretteleggingFOMListe().forEach(fom -> {
            if (fom.getKilde() == null) {
                var kilde = utledKildeForTilr(fom, svpTilrettelegging, opprinneligeTilr, behandling);
                tilretteleggingDatoDtos.add(
                    new SvpTilretteleggingDatoDto(fom.getFomDato(), fom.getType(), fom.getStillingsprosent(), fom.getOverstyrtUtbetalingsgrad(),
                        kilde, fom.getTidligstMotattDato()));
            } else {
                tilretteleggingDatoDtos.add(
                    new SvpTilretteleggingDatoDto(fom.getFomDato(), fom.getType(), fom.getStillingsprosent(), fom.getOverstyrtUtbetalingsgrad(),
                        fom.getKilde(), fom.getTidligstMotattDato()));
            }
        });
        return tilretteleggingDatoDtos;
    }

    private SvpTilretteleggingFomKilde utledKildeForTilr(TilretteleggingFOM eksFom,
                                                         SvpTilretteleggingEntitet svpTilrettelegging,
                                                         List<SvpTilretteleggingEntitet> opprinneligeTilr,
                                                         Behandling behandling) {
        Optional<TilretteleggingFOM> eksFomFinnesIOpprinneligGrunnlag = opprinneligeTilr.stream()
            .filter(opprTilr -> opprTilr.getId().equals(svpTilrettelegging.getId()))
            .flatMap(mathendeTilr -> mathendeTilr.getTilretteleggingFOMListe().stream())
            .filter(opprFom -> opprFom.equals(eksFom))
            .findFirst();

        if (Boolean.TRUE.equals(svpTilrettelegging.getKopiertFraTidligereBehandling())) {
            if (behandling.erRevurdering() && eksFomFinnesIOpprinneligGrunnlag.isPresent() && behandling.getOpprettetDato()
                .toLocalDate()
                .equals(eksFomFinnesIOpprinneligGrunnlag.get().getTidligstMotattDato())) {
                return SvpTilretteleggingFomKilde.SØKNAD;
            }
            return SvpTilretteleggingFomKilde.TIDLIGERE_VEDTAK;
        } else if (eksFomFinnesIOpprinneligGrunnlag.isPresent()) {
            return SvpTilretteleggingFomKilde.SØKNAD;
        } else {
            return SvpTilretteleggingFomKilde.ENDRET_AV_SAKSBEHANDLER;
        }
    }

    private List<SvpAvklartOppholdPeriodeDto> mapAvklartOppholdPeriode(SvpTilretteleggingEntitet svpTilrettelegging) {
        var liste = svpTilrettelegging.getAvklarteOpphold()
            .stream()
            .map(avklartOpphold -> {
                var kilde = switch (avklartOpphold.getKilde()) {
                    case SØKNAD -> SvpAvklartOppholdPeriodeDto.SvpOppholdKilde.SØKNAD;
                    case REGISTRERT_AV_SAKSBEHANDLER -> SvpAvklartOppholdPeriodeDto.SvpOppholdKilde.REGISTRERT_AV_SAKSBEHANDLER;
                    case null -> SvpAvklartOppholdPeriodeDto.SvpOppholdKilde.REGISTRERT_AV_SAKSBEHANDLER;
                };
                return new SvpAvklartOppholdPeriodeDto(avklartOpphold.getFom(), avklartOpphold.getTom(), avklartOpphold.getOppholdÅrsak(),
                    kilde, false);
            })
            .toList();
        return new ArrayList<>(liste);
    }

    private List<SvpAvklartOppholdPeriodeDto> hentFerieFraIM(Inntektsmelding inntektsmeldingForArbeidsforhold) {
        List<SvpAvklartOppholdPeriodeDto> ferieListe = new ArrayList<>();
        inntektsmeldingForArbeidsforhold.getUtsettelsePerioder()
            .stream()
            .filter(utsettelse -> UtsettelseÅrsak.FERIE.equals(utsettelse.getÅrsak()))
            .forEach(utsettelse -> ferieListe.add(
                new SvpAvklartOppholdPeriodeDto(utsettelse.getPeriode().getFomDato(), utsettelse.getPeriode().getTomDato(), SvpOppholdÅrsak.FERIE,
                    SvpAvklartOppholdPeriodeDto.SvpOppholdKilde.INNTEKTSMELDING, true)));
        return ferieListe;
    }
}
