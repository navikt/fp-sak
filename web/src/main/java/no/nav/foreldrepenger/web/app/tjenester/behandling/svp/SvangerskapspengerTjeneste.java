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
import java.util.Set;

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
import no.nav.foreldrepenger.domene.iay.modell.ArbeidsforholdOverstyrtePerioder;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.foreldrepenger.domene.iay.modell.Inntektsmelding;
import no.nav.foreldrepenger.domene.iay.modell.Permisjon;
import no.nav.foreldrepenger.domene.iay.modell.Yrkesaktivitet;
import no.nav.foreldrepenger.domene.iay.modell.YrkesaktivitetFilter;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.ArbeidsforholdHandlingType;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.BekreftetPermisjonStatus;
import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;
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
        var iayGrunnlag = iayTjeneste.finnGrunnlag(behandlingId);
        var arbeidsforholdInformasjon = iayGrunnlag.flatMap(InntektArbeidYtelseGrunnlag::getArbeidsforholdInformasjon);
        var registerFilter = iayGrunnlag.map(iay -> new YrkesaktivitetFilter(iay.getArbeidsforholdInformasjon(), iay.getAktørArbeidFraRegister(aktørId)));
        var saksbehandletFilter = iayGrunnlag.map(iay -> new YrkesaktivitetFilter(iay.getArbeidsforholdInformasjon(),
            finnSaksbehandletHvisEksisterer(aktørId, iay)));
        var stp = skjæringstidspunktTjeneste.getSkjæringstidspunkter(behandlingId);
        var inntektsmeldinger = inntektsmeldingTjeneste.hentInntektsmeldinger(BehandlingReferanse.fra(behandling),
                stp.getUtledetSkjæringstidspunkt());
        var iayOverstyringer = iayGrunnlag.map(InntektArbeidYtelseGrunnlag::getArbeidsforholdOverstyringer).orElseGet(List::of);

        gjeldendeTilrettelegginger.forEach(tilr -> {
            var arbeidsforholdDto = mapArbeidsforholdDto(tilr, behandling, opprinneligeTilrettelegginger, inntektsmeldinger, registerFilter,
                saksbehandletFilter, arbeidsforholdInformasjon, iayOverstyringer);
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

    public Optional<BigDecimal> utledStillingsprosentForTilrPeriode(YrkesaktivitetFilter registerFilter, List<ArbeidsforholdOverstyring> overstyringer, SvpTilretteleggingEntitet tilrettelegging) {
        if (ArbeidType.ORDINÆRT_ARBEIDSFORHOLD.equals(tilrettelegging.getArbeidType())) {
            var førsteTilrStartDato = tilrettelegging.getTilretteleggingFOMListe()
                .stream()
                .map(TilretteleggingFOM::getFomDato)
                .min(Comparator.naturalOrder())
                .orElse(null);

            var yrkesaktiviteter = registerFilter.getYrkesaktiviteter()
                .stream()
                .filter(ya -> Objects.equals(ya.getArbeidsgiver(), tilrettelegging.getArbeidsgiver().orElse(null))
                    && tilrettelegging.getInternArbeidsforholdRef().orElse(InternArbeidsforholdRef.nullRef()).gjelderFor(ya.getArbeidsforholdRef()))
                .toList();

            if (yrkesaktiviteter.isEmpty() || førsteTilrStartDato == null) {
                return Optional.of(BigDecimal.ZERO);
            }

            var stillingsprosent = yrkesaktiviteter.stream()
                .map(yrkesaktivitet -> finnStillingsprosentForDato(yrkesaktivitet, førsteTilrStartDato, finnOverstyring(yrkesaktivitet.getArbeidsgiver(), yrkesaktivitet.getArbeidsforholdRef(), overstyringer).orElse(null)))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

            if (stillingsprosent.compareTo(BigDecimal.valueOf(100)) > 0) {
                LOG.info("SvangerskapspengerTjeneste: utledStillingsprosentForTilrPeriode: Stillingsprosent over 100% for tilrettelegging med id:{} og arbeidsgiver:{}, stillingsprosent:{}", tilrettelegging.getId(), tilrettelegging.getArbeidsgiver(), stillingsprosent);
            }
            return Optional.of(stillingsprosent);
        } else {
            return Optional.empty();
        }
    }

    private static Optional<ArbeidsforholdOverstyring> finnOverstyring(Arbeidsgiver arbeidsgiver,
                                                                       InternArbeidsforholdRef arbeidsforholdRef,
                                                                       List<ArbeidsforholdOverstyring> overstyringer) {
        return overstyringer.stream()
            .filter(os -> Objects.equals(os.getArbeidsgiver(), arbeidsgiver) && os.getArbeidsforholdRef().gjelderFor(arbeidsforholdRef))
            .findFirst();
    }

    private BigDecimal finnStillingsprosentForDato(Yrkesaktivitet yrkesaktivitet, LocalDate førsteTilrStartDato, ArbeidsforholdOverstyring overstyring) {
        var ansettelsePeriode = finnRelevantAnsettelsesperiode(yrkesaktivitet, overstyring, førsteTilrStartDato);

        return ansettelsePeriode.map(periode ->
            yrkesaktivitet.getAlleAktivitetsAvtaler()
                .stream()
                .filter(aa -> !aa.erAnsettelsesPeriode() && aa.getPeriode().overlapper(periode))
                .filter(aa -> aa.getProsentsats() != null && aa.getProsentsats().getVerdi() != null)
                .max(Comparator.comparing(AktivitetsAvtale::getPeriode))
                .map(AktivitetsAvtale::getProsentsats)
                .map(Stillingsprosent::getVerdi)
                .orElse(BigDecimal.ZERO))
            .orElse(BigDecimal.ZERO);
    }

    private static Optional<DatoIntervallEntitet> finnRelevantAnsettelsesperiode(Yrkesaktivitet ya,
                                                                                 ArbeidsforholdOverstyring arbeidsforholdOverstyring,
                                                                                 LocalDate førsteTilrStartDato) {
        if (arbeidsforholdOverstyring != null && ArbeidsforholdHandlingType.BRUK_MED_OVERSTYRT_PERIODE.equals(arbeidsforholdOverstyring.getHandling())) {
                return arbeidsforholdOverstyring.getArbeidsforholdOverstyrtePerioder()
                    .stream().map(ArbeidsforholdOverstyrtePerioder::getOverstyrtePeriode)
                    .max(Comparator.comparing(DatoIntervallEntitet::getTomDato));
        }

        return ya.getAlleAktivitetsAvtaler().stream()
            .filter(AktivitetsAvtale::erAnsettelsesPeriode)
            .filter(aa -> aa.getPeriode().inkluderer(førsteTilrStartDato.minusDays(1)) || aa.getPeriode().getFomDato().isAfter(førsteTilrStartDato.minusDays(1)))
            .map(AktivitetsAvtale::getPeriode)
            .max(DatoIntervallEntitet::compareTo);
    }

    private Optional<AktørArbeid> finnSaksbehandletHvisEksisterer(AktørId aktørId, InntektArbeidYtelseGrunnlag g) {
        if (g.harBlittSaksbehandlet()) {
            return g.getSaksbehandletVersjon()
                .flatMap(aggregat -> aggregat.getAktørArbeid().stream().filter(aa -> aa.getAktørId().equals(aktørId)).findFirst());
        }
        return Optional.empty();
    }

    private boolean erTilgjengeligForBeregning(SvpTilretteleggingEntitet tilr, Optional<YrkesaktivitetFilter> filter) {
        if (tilr.getArbeidsgiver().isEmpty()) {
            return true;
        }
        var yaBeregning = filter.map(YrkesaktivitetFilter::getYrkesaktiviteterForBeregning).orElseGet(Set::of);
        if (yaBeregning.isEmpty()) {
            return false;
        }
        return yaBeregning.stream()
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
                                                      Optional<YrkesaktivitetFilter> registerFilter,
                                                      Optional<YrkesaktivitetFilter> saksbehandletFilter,
                                                      Optional<ArbeidsforholdInformasjon> arbeidsforholdInformasjon,
                                                      List<ArbeidsforholdOverstyring> overstyringer) {

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
        registerFilter.flatMap(rf -> utledStillingsprosentForTilrPeriode(rf, overstyringer, svpTilrettelegging)).ifPresent(arbeidsforholdDto::setStillingsprosentStartTilrettelegging);
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

    private Optional<String> finnEksternRef(SvpTilretteleggingEntitet svpTilrettelegging, Optional<ArbeidsforholdInformasjon> arbeidsforholdInformasjon) {
        return svpTilrettelegging.getInternArbeidsforholdRef().map(ref -> {
            var arbeidsgiver = svpTilrettelegging.getArbeidsgiver()
                .orElseThrow(() -> new IllegalStateException(
                    "Utviklerfeil: Fant ikke forventent arbeidsgiver for tilrettelegging: " + svpTilrettelegging.getId()));
            return arbeidsforholdInformasjon.map(ai -> ai.finnEkstern(arbeidsgiver, ref).getReferanse()).orElse(null);
        });
    }

    private List<VelferdspermisjonDto> finnRelevanteVelferdspermisjoner(SvpTilretteleggingEntitet svpTilrettelegging,
                                                                        Optional<YrkesaktivitetFilter> registerFilter,
                                                                        Optional<YrkesaktivitetFilter> saksbehandletFilter) {
        return svpTilrettelegging.getArbeidsgiver()
            .map(a -> mapVelferdspermisjoner(svpTilrettelegging, registerFilter, a, saksbehandletFilter))
            .orElse(Collections.emptyList());
    }

    private List<VelferdspermisjonDto> mapVelferdspermisjoner(SvpTilretteleggingEntitet svpTilrettelegging,
                                                              Optional<YrkesaktivitetFilter> registerFilter,
                                                              Arbeidsgiver arbeidsgiver,
                                                              Optional<YrkesaktivitetFilter> saksbehandletFilter) {
        return registerFilter.map(YrkesaktivitetFilter::getYrkesaktiviteter).orElseGet(Set::of)
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

    private VelferdspermisjonDto mapPermisjon(Permisjon p, Optional<YrkesaktivitetFilter> registerFilter, Optional<YrkesaktivitetFilter> saksbehandletFilter) {
        return new VelferdspermisjonDto(p.getFraOgMed(),
            p.getTilOgMed() == null || p.getTilOgMed().isEqual(Tid.TIDENES_ENDE) ? null : p.getTilOgMed(), p.getProsentsats().getVerdi(),
            p.getPermisjonsbeskrivelseType(), erGyldig(p, registerFilter, saksbehandletFilter));
    }

    private Boolean erGyldig(Permisjon p, Optional<YrkesaktivitetFilter> yrkesfilter, Optional<YrkesaktivitetFilter> saksbehandletFilter) {
        var arbeidsgiver = p.getYrkesaktivitet().getArbeidsgiver();
        var arbeidsforholdRef = p.getYrkesaktivitet().getArbeidsforholdRef();
        var saksbehandletAktivitet = saksbehandletFilter.map(YrkesaktivitetFilter::getYrkesaktiviteter).orElseGet(Set::of)
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
            var bekreftetPermisjonValg = yrkesfilter.map(YrkesaktivitetFilter::getArbeidsforholdOverstyringer).orElseGet(Set::of)
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
                    case TIDLIGERE_VEDTAK -> SvpAvklartOppholdPeriodeDto.SvpOppholdKilde.TIDLIGERE_VEDTAK;
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
