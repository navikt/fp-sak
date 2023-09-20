package no.nav.foreldrepenger.web.app.tjenester.behandling.svp;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
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
import no.nav.foreldrepenger.domene.iay.modell.*;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.BekreftetPermisjonStatus;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktTjeneste;
import no.nav.vedtak.konfig.Tid;

import java.util.*;

import static no.nav.foreldrepenger.domene.arbeidInntektsmelding.HåndterePermisjoner.harRelevantPermisjonSomOverlapperTilretteleggingFom;

@ApplicationScoped
public class SvangerskapspengerTjeneste {

    private static final Map<ArbeidType, UttakArbeidType> ARBTYPE_MAP = Map.ofEntries(
        Map.entry(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD, UttakArbeidType.ORDINÆRT_ARBEID),
        Map.entry(ArbeidType.FRILANSER, UttakArbeidType.FRILANS),
        Map.entry(ArbeidType.SELVSTENDIG_NÆRINGSDRIVENDE, UttakArbeidType.SELVSTENDIG_NÆRINGSDRIVENDE)
    );

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

        var dto = new SvpTilretteleggingDto();

        var familieHendelseGrunnlag = familieHendelseRepository.hentAggregatHvisEksisterer(behandlingId);
        if (familieHendelseGrunnlag.isEmpty()) {
            return dto;
        }

        var terminbekreftelse = familieHendelseGrunnlag.get().getGjeldendeTerminbekreftelse();
        if (terminbekreftelse.isEmpty()) {
            throw SvangerskapsTjenesteFeil.kanIkkeFinneTerminbekreftelsePåSvangerskapspengerSøknad(behandlingId);
        }
        dto.setTermindato(terminbekreftelse.get().getTermindato());
        familieHendelseGrunnlag.get().getGjeldendeVersjon().getFødselsdato().ifPresent(dto::setFødselsdato);

        var gjeldendeTilrettelegginger = svangerskapspengerRepository.hentGrunnlag(behandlingId)
            .map(SvpGrunnlagEntitet::getGjeldendeVersjon)
            .map(SvpTilretteleggingerEntitet::getTilretteleggingListe)
            .orElseThrow(() -> SvangerskapsTjenesteFeil.kanIkkeFinneSvangerskapspengerGrunnlagForBehandling(behandlingId));

        var opprinneligeTilrettelegginger = svangerskapspengerRepository.hentGrunnlag(behandlingId)
            .map(SvpGrunnlagEntitet::getOpprinneligeTilrettelegginger)
            .map(SvpTilretteleggingerEntitet::getTilretteleggingListe).orElse(Collections.emptyList());

        var iayGrunnlag = iayTjeneste.hentGrunnlag(behandlingId);
        var arbeidsforholdInformasjon = iayGrunnlag.getArbeidsforholdInformasjon()
            .orElseThrow(() -> new IllegalStateException("Utviklerfeil: Fant ikke forventent arbeidsforholdinformasjon for behandling: " + behandlingId));

        var registerFilter = new YrkesaktivitetFilter(iayGrunnlag.getArbeidsforholdInformasjon(), iayGrunnlag.getAktørArbeidFraRegister(behandling.getAktørId()));
        var saksbehandletFilter = new YrkesaktivitetFilter(iayGrunnlag.getArbeidsforholdInformasjon(), finnSaksbehandletHvisEksisterer(behandling.getAktørId(), iayGrunnlag));

        var inntektsmeldinger = inntektsmeldingTjeneste.hentInntektsmeldinger(BehandlingReferanse.fra(behandling), skjæringstidspunktTjeneste.getSkjæringstidspunkter(behandlingId).getSkjæringstidspunktOpptjening());

        gjeldendeTilrettelegginger.forEach(tilr -> {
            var tilretteleggingDto = mapTilretteleggingsinfo(tilr, inntektsmeldinger, opprinneligeTilrettelegginger, behandling);
            tilretteleggingDto.setVelferdspermisjoner(finnRelevanteVelferdspermisjoner(tilr, registerFilter, saksbehandletFilter));
            finnEksternRef(tilr, arbeidsforholdInformasjon).ifPresent(tilretteleggingDto::setEksternArbeidsforholdReferanse);
            tilretteleggingDto.setKanTilrettelegges(erTilgjengeligForBeregning(tilr, registerFilter));
            dto.leggTilArbeidsforhold(tilretteleggingDto);
        });
        dto.setSaksbehandlet(harSaksbehandletTilrettelegging(behandling));

        return dto;
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
        return filter.getYrkesaktiviteterForBeregning().stream()
            .anyMatch(ya -> Objects.equals(ya.getArbeidsgiver(), tilr.getArbeidsgiver().orElse(null))
                && tilr.getInternArbeidsforholdRef().orElse(InternArbeidsforholdRef.nullRef()).gjelderFor(ya.getArbeidsforholdRef()));
        }

    /**
     * Må se på aksjonspunkt ettersom gjeldende tilrettelegginger ikke bare brukes av saksbehandler
     */
    private boolean harSaksbehandletTilrettelegging(Behandling behandling) {
        var aksjonspunkt = behandling.getAksjonspunktMedDefinisjonOptional(AksjonspunktDefinisjon.VURDER_SVP_TILRETTELEGGING);
        return aksjonspunkt.isPresent() && aksjonspunkt.get().erUtført();
    }

    private SvpArbeidsforholdDto mapTilretteleggingsinfo(SvpTilretteleggingEntitet svpTilrettelegging, List<Inntektsmelding> inntektsmeldinger, List<SvpTilretteleggingEntitet> opprinneligeTilr,
                                                         Behandling behandling) {
        var dto = new SvpArbeidsforholdDto();
        dto.setTilretteleggingId(svpTilrettelegging.getId());
        dto.setTilretteleggingBehovFom(svpTilrettelegging.getBehovForTilretteleggingFom());
        dto.setTilretteleggingDatoer(utledTilretteleggingDatoer(svpTilrettelegging, opprinneligeTilr, behandling));
        dto.setAvklarteOppholdPerioder(mapAvklartOppholdPeriode(svpTilrettelegging));
        // Ferie fra inntektsmelding skal vises til saksbehandler hvis finnes
        svpTilrettelegging.getArbeidsgiver()
            .flatMap(arbeidsgiver -> finnIMForArbeidsforhold(inntektsmeldinger, arbeidsgiver, svpTilrettelegging.getInternArbeidsforholdRef().orElse(InternArbeidsforholdRef.nullRef())))
            .ifPresent(im -> dto.leggTilOppholdPerioder(hentFerieFraIM(im)));
        dto.setOpplysningerOmRisiko(svpTilrettelegging.getOpplysningerOmRisikofaktorer().orElse(null));
        dto.setOpplysningerOmTilrettelegging(svpTilrettelegging.getOpplysningerOmTilretteleggingstiltak().orElse(null));
        dto.setBegrunnelse(svpTilrettelegging.getBegrunnelse().orElse(null));
        dto.setKopiertFraTidligereBehandling(svpTilrettelegging.getKopiertFraTidligereBehandling());
        dto.setMottattTidspunkt(svpTilrettelegging.getMottattTidspunkt());
        dto.setSkalBrukes(svpTilrettelegging.getSkalBrukes());
        dto.setUttakArbeidType(ARBTYPE_MAP.getOrDefault(svpTilrettelegging.getArbeidType(), UttakArbeidType.ANNET));
        svpTilrettelegging.getArbeidsgiver().ifPresent(ag -> dto.setArbeidsgiverReferanse(ag.getIdentifikator()));
        svpTilrettelegging.getInternArbeidsforholdRef().ifPresent(ref -> dto.setInternArbeidsforholdReferanse(ref.getReferanse()));
        return dto;
    }

    private Optional<Inntektsmelding> finnIMForArbeidsforhold(List<Inntektsmelding> inntektsmeldinger, Arbeidsgiver arbeidsgiver, InternArbeidsforholdRef internArbeidsforholdRef) {
        return inntektsmeldinger.stream()
            .filter(im -> im.getArbeidsgiver().equals(arbeidsgiver) && im.getArbeidsforholdRef().gjelderFor(internArbeidsforholdRef))
            .findFirst();
    }

    private Optional<String> finnEksternRef(SvpTilretteleggingEntitet svpTilrettelegging, ArbeidsforholdInformasjon arbeidsforholdInformasjon) {
        return svpTilrettelegging.getInternArbeidsforholdRef().map(ref -> {
            var arbeidsgiver = svpTilrettelegging.getArbeidsgiver()
                .orElseThrow(() -> new IllegalStateException("Utviklerfeil: Fant ikke forventent arbeidsgiver for tilrettelegging: " + svpTilrettelegging.getId()));
            return arbeidsforholdInformasjon.finnEkstern(arbeidsgiver, ref).getReferanse();
        });
    }

    private List<VelferdspermisjonDto> finnRelevanteVelferdspermisjoner(SvpTilretteleggingEntitet svpTilrettelegging, YrkesaktivitetFilter registerFilter, YrkesaktivitetFilter saksbehandletFilter) {
        return svpTilrettelegging.getArbeidsgiver().map(a -> mapVelferdspermisjoner(svpTilrettelegging, registerFilter, a, saksbehandletFilter)).orElse(Collections.emptyList());
    }

    private List<VelferdspermisjonDto> mapVelferdspermisjoner(SvpTilretteleggingEntitet svpTilrettelegging, YrkesaktivitetFilter registerFilter, Arbeidsgiver arbeidsgiver, YrkesaktivitetFilter saksbehandletFilter) {
        return registerFilter.getYrkesaktiviteter().stream()
            .filter(ya -> erSammeArbeidsgiver(ya, arbeidsgiver, svpTilrettelegging))
            .filter( ya -> harRelevantPermisjonSomOverlapperTilretteleggingFom(ya, svpTilrettelegging.getBehovForTilretteleggingFom() ))
            .flatMap(ya -> ya.getPermisjon().stream())
            .map(p -> mapPermisjon(p, registerFilter, saksbehandletFilter))
            .toList();
    }

    private boolean erSammeArbeidsgiver(Yrkesaktivitet yrkesaktivitet, Arbeidsgiver arbeidsgiver, SvpTilretteleggingEntitet svpTilrettelegging) {
        return yrkesaktivitet.getArbeidsgiver() != null && yrkesaktivitet.getArbeidsgiver().getIdentifikator().equals(arbeidsgiver.getIdentifikator())
            && svpTilrettelegging.getInternArbeidsforholdRef().orElse(InternArbeidsforholdRef.nullRef()).gjelderFor(yrkesaktivitet.getArbeidsforholdRef());
    }

    private VelferdspermisjonDto mapPermisjon(Permisjon p, YrkesaktivitetFilter registerFilter, YrkesaktivitetFilter saksbehandletFilter) {
        return new VelferdspermisjonDto(p.getFraOgMed(),
            p.getTilOgMed() == null || p.getTilOgMed().isEqual(Tid.TIDENES_ENDE) ? null : p.getTilOgMed(),
            p.getProsentsats().getVerdi(),
            p.getPermisjonsbeskrivelseType(),
            erGyldig(p, registerFilter, saksbehandletFilter));
    }

    private Boolean erGyldig(Permisjon p, YrkesaktivitetFilter yrkesfilter, YrkesaktivitetFilter saksbehandletFilter) {
        var arbeidsgiver = p.getYrkesaktivitet().getArbeidsgiver();
        var arbeidsforholdRef = p.getYrkesaktivitet().getArbeidsforholdRef();
        var saksbehandletAktivitet = saksbehandletFilter.getYrkesaktiviteter().stream()
            .filter(ya -> ya.getArbeidsgiver() != null && ya.getArbeidsgiver().getIdentifikator().equals(arbeidsgiver.getIdentifikator())
                && ya.getArbeidsforholdRef().gjelderFor(arbeidsforholdRef)).findFirst();
        if (saksbehandletAktivitet.isPresent()) {
            // I svangerskapspenger ble permisjonsvalg før lagret på saksbehandlet versjon. Dette er nå endret til å lagres på arbeidsforholdinformasjon
            var saksbehandletPermisjon = saksbehandletAktivitet.get().getPermisjon();
            return saksbehandletPermisjon.stream().anyMatch(sp -> sp.getPermisjonsbeskrivelseType().equals(p.getPermisjonsbeskrivelseType())
                && sp.getFraOgMed().isEqual(p.getFraOgMed())
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

    private List<SvpTilretteleggingDatoDto> utledTilretteleggingDatoer(SvpTilretteleggingEntitet svpTilrettelegging, List<SvpTilretteleggingEntitet> opprinneligeTilr, Behandling behandling) {
        List<SvpTilretteleggingDatoDto> tilretteleggingDatoDtos = new ArrayList<>();
        svpTilrettelegging.getTilretteleggingFOMListe().forEach(fom -> {
            if (fom.getKilde()== null) {
                var kilde = utledKildeForTilr(fom, svpTilrettelegging, opprinneligeTilr, behandling);
                tilretteleggingDatoDtos.add(new SvpTilretteleggingDatoDto(fom.getFomDato(), fom.getType(), fom.getStillingsprosent(), fom.getOverstyrtUtbetalingsgrad(), kilde, fom.getTidligstMotattDato()));
            } else {
                tilretteleggingDatoDtos.add(
                    new SvpTilretteleggingDatoDto(fom.getFomDato(), fom.getType(), fom.getStillingsprosent(), fom.getOverstyrtUtbetalingsgrad(), fom.getKilde(), fom.getTidligstMotattDato()));
            }
        });
        return tilretteleggingDatoDtos;
    }

    private SvpTilretteleggingFomKilde utledKildeForTilr(TilretteleggingFOM eksFom, SvpTilretteleggingEntitet svpTilrettelegging, List<SvpTilretteleggingEntitet> opprinneligeTilr, Behandling behandling) {
        Optional<TilretteleggingFOM> eksFomFinnesIOpprinneligGrunnlag = opprinneligeTilr.stream()
            .filter(opprTilr -> opprTilr.getId().equals(svpTilrettelegging.getId()))
            .flatMap(mathendeTilr -> mathendeTilr.getTilretteleggingFOMListe().stream())
            .filter(opprFom -> opprFom.equals(eksFom)).findFirst();

        if (Boolean.TRUE.equals(svpTilrettelegging.getKopiertFraTidligereBehandling())) {
            if (behandling.erRevurdering() && eksFomFinnesIOpprinneligGrunnlag.isPresent() && behandling.getOpprettetDato().toLocalDate().equals(eksFomFinnesIOpprinneligGrunnlag.get().getTidligstMotattDato())) {
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
        var liste = svpTilrettelegging.getAvklarteOpphold().stream()
            .map(avklartOpphold -> new SvpAvklartOppholdPeriodeDto(avklartOpphold.getFom(), avklartOpphold.getTom(), avklartOpphold.getOppholdÅrsak(), false))
            .toList();
        return new ArrayList<>(liste);
    }

    private List<SvpAvklartOppholdPeriodeDto> hentFerieFraIM(Inntektsmelding inntektsmeldingForArbeidsforhold) {
        List<SvpAvklartOppholdPeriodeDto> ferieListe = new ArrayList<>();
        inntektsmeldingForArbeidsforhold.getUtsettelsePerioder().stream()
            .filter(utsettelse -> UtsettelseÅrsak.FERIE.equals(utsettelse.getÅrsak()))
            .forEach(utsettelse -> ferieListe.add(new SvpAvklartOppholdPeriodeDto(utsettelse.getPeriode().getFomDato(), utsettelse.getPeriode().getTomDato(), SvpOppholdÅrsak.FERIE, true)));
        return ferieListe;
    }
}
