package no.nav.foreldrepenger.web.app.tjenester.behandling.svp;

import static no.nav.foreldrepenger.domene.arbeidInntektsmelding.HåndterePermisjoner.harRelevantPermisjonSomOverlapperTilretteleggingFom;
import static no.nav.foreldrepenger.domene.tid.AbstractLocalDateInterval.TIDENES_ENDE;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.SvangerskapspengerRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.SvpGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.SvpTilretteleggingEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.SvpTilretteleggingerEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.UttakArbeidType;
import no.nav.foreldrepenger.behandlingslager.virksomhet.ArbeidType;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.iay.modell.ArbeidsforholdInformasjon;
import no.nav.foreldrepenger.domene.iay.modell.Permisjon;
import no.nav.foreldrepenger.domene.iay.modell.Yrkesaktivitet;
import no.nav.foreldrepenger.domene.iay.modell.YrkesaktivitetFilter;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;

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

    public SvangerskapspengerTjeneste() {
        //CDI greier
    }

    @Inject
    public SvangerskapspengerTjeneste(SvangerskapspengerRepository svangerskapspengerRepository,
                                      FamilieHendelseRepository familieHendelseRepository,
                                      InntektArbeidYtelseTjeneste iayTjeneste) {
        this.svangerskapspengerRepository = svangerskapspengerRepository;
        this.familieHendelseRepository = familieHendelseRepository;
        this.iayTjeneste = iayTjeneste;
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

        var iayGrunnlag = iayTjeneste.hentGrunnlag(behandlingId);
        var arbeidsforholdInformasjon = iayGrunnlag.getArbeidsforholdInformasjon()
            .orElseThrow(() -> new IllegalStateException("Utviklerfeil: Fant ikke forventent arbeidsforholdinformasjon for behandling: " + behandlingId));

        var filter = new YrkesaktivitetFilter(iayGrunnlag.getArbeidsforholdInformasjon(), iayGrunnlag.getAktørArbeidFraRegister(behandling.getAktørId()));
        var saksbehandletVersjon = iayGrunnlag.getSaksbehandletVersjon();
        var saksbehandletFilter = new YrkesaktivitetFilter(iayGrunnlag.getArbeidsforholdInformasjon(), saksbehandletVersjon.flatMap(iay -> iay.getAktørArbeid().stream().filter(aa -> aa.getAktørId().equals(behandling.getAktørId())).findFirst()));

        gjeldendeTilrettelegginger.forEach(tilr -> {
            SvpArbeidsforholdDto tilretteleggingDto = mapTilretteleggingsinfo(tilr);
            tilretteleggingDto.setVelferdspermisjoner(finnRelevanteVelferdspermisjoner(tilr, filter, saksbehandletFilter));
            finnEksternRef(tilr, arbeidsforholdInformasjon).ifPresent(tilretteleggingDto::setEksternArbeidsforholdReferanse);
            tilretteleggingDto.setKanTilrettelegges(erTilgjengeligForBeregning(tilr, filter));
            dto.leggTilArbeidsforhold(tilretteleggingDto);
        });
        dto.setSaksbehandlet(harSaksbehandletTilrettelegging(behandling));

        return dto;
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

    private SvpArbeidsforholdDto mapTilretteleggingsinfo(SvpTilretteleggingEntitet svpTilrettelegging) {
        var dto = new SvpArbeidsforholdDto();
        dto.setTilretteleggingId(svpTilrettelegging.getId());
        dto.setTilretteleggingBehovFom(svpTilrettelegging.getBehovForTilretteleggingFom());
        dto.setTilretteleggingDatoer(utledTilretteleggingDatoer(svpTilrettelegging));
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

    private Optional<String> finnEksternRef(SvpTilretteleggingEntitet svpTilrettelegging, ArbeidsforholdInformasjon arbeidsforholdInformasjon) {
        return svpTilrettelegging.getInternArbeidsforholdRef().map(ref -> {
            var arbeidsgiver = svpTilrettelegging.getArbeidsgiver()
                .orElseThrow(() -> new IllegalStateException("Utviklerfeil: Fant ikke forventent arbeidsgiver for tilrettelegging: " + svpTilrettelegging.getId()));
            return arbeidsforholdInformasjon.finnEkstern(arbeidsgiver, ref).getReferanse();
        });
    }

    private List<VelferdspermisjonDto> finnRelevanteVelferdspermisjoner(SvpTilretteleggingEntitet svpTilrettelegging, YrkesaktivitetFilter filter, YrkesaktivitetFilter saksbehandletFilter) {
        return svpTilrettelegging.getArbeidsgiver().map(a -> mapVelferdspermisjoner(svpTilrettelegging, filter, a, saksbehandletFilter)).orElse(Collections.emptyList());
    }

    private List<VelferdspermisjonDto> mapVelferdspermisjoner(SvpTilretteleggingEntitet svpTilrettelegging, YrkesaktivitetFilter filter, Arbeidsgiver arbeidsgiver, YrkesaktivitetFilter saksbehandletFilter) {
        return filter.getYrkesaktiviteter().stream()
            .filter(ya -> erSammeArbeidsgiver(ya, arbeidsgiver, svpTilrettelegging))
            .filter( ya -> harRelevantPermisjonSomOverlapperTilretteleggingFom(ya, svpTilrettelegging.getBehovForTilretteleggingFom() ))
            .flatMap(ya -> ya.getPermisjon().stream())
            .map(p -> mapPermisjon(p, saksbehandletFilter))
            .collect(Collectors.toList());
    }

    private boolean erSammeArbeidsgiver(Yrkesaktivitet yrkesaktivitet, Arbeidsgiver arbeidsgiver, SvpTilretteleggingEntitet svpTilrettelegging) {
        return yrkesaktivitet.getArbeidsgiver() != null && yrkesaktivitet.getArbeidsgiver().getIdentifikator().equals(arbeidsgiver.getIdentifikator())
            && svpTilrettelegging.getInternArbeidsforholdRef().orElse(InternArbeidsforholdRef.nullRef()).gjelderFor(yrkesaktivitet.getArbeidsforholdRef());
    }

    private VelferdspermisjonDto mapPermisjon(Permisjon p, YrkesaktivitetFilter saksbehandletFilter) {
        var saksbehandletAktivitet = saksbehandletFilter.getYrkesaktiviteter().stream()
            .filter(ya -> {
                var yrkesaktivitet = p.getYrkesaktivitet();
                return ya.getArbeidsgiver() != null && ya.getArbeidsgiver().getIdentifikator().equals(p.getYrkesaktivitet().getArbeidsgiver().getIdentifikator())
                    && ya.getArbeidsforholdRef().gjelderFor(yrkesaktivitet.getArbeidsforholdRef());
            }).findFirst();
        return new VelferdspermisjonDto(p.getFraOgMed(),
            p.getTilOgMed() == null || p.getTilOgMed().isEqual(TIDENES_ENDE) ? null : p.getTilOgMed(),
            p.getProsentsats().getVerdi(),
            p.getPermisjonsbeskrivelseType(),
            erGyldig(p, saksbehandletAktivitet));
    }

    private Boolean erGyldig(Permisjon p, Optional<Yrkesaktivitet> saksbehandletAktivitet) {
        if (saksbehandletAktivitet.isEmpty()) {
            return null;
        }
        var saksbehandletPermisjon = saksbehandletAktivitet.get().getPermisjon();
        return saksbehandletPermisjon.stream().anyMatch(sp -> sp.getPermisjonsbeskrivelseType().equals(p.getPermisjonsbeskrivelseType())
        && sp.getFraOgMed().isEqual(p.getFraOgMed())
        && sp.getProsentsats().getVerdi().compareTo(p.getProsentsats().getVerdi()) == 0);
    }



    private List<SvpTilretteleggingDatoDto> utledTilretteleggingDatoer(SvpTilretteleggingEntitet svpTilrettelegging) {
        return svpTilrettelegging.getTilretteleggingFOMListe().stream()
            .map(fom -> new SvpTilretteleggingDatoDto(fom.getFomDato(), fom.getType(), fom.getStillingsprosent(), fom.getOverstyrtUtbetalingsgrad()))
            .collect(Collectors.toList());
    }
}
