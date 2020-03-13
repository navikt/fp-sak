package no.nav.foreldrepenger.web.app.tjenester.behandling.svp;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.TerminbekreftelseEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.SvangerskapspengerRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.SvpGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.SvpTilretteleggingEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.TilretteleggingFilter;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.arbeidsgiver.ArbeidsgiverOpplysninger;
import no.nav.foreldrepenger.domene.arbeidsgiver.ArbeidsgiverTjeneste;
import no.nav.foreldrepenger.domene.iay.modell.ArbeidsforholdInformasjon;

@ApplicationScoped
public class SvangerskapspengerTjeneste {

    private ArbeidsgiverTjeneste arbeidsgiverTjeneste;
    private SvangerskapspengerRepository svangerskapspengerRepository;
    private FamilieHendelseRepository familieHendelseRepository;
    private InntektArbeidYtelseTjeneste iayTjeneste;

    public SvangerskapspengerTjeneste() {
        //CDI greier
    }

    @Inject
    public SvangerskapspengerTjeneste(SvangerskapspengerRepository svangerskapspengerRepository,
                                      ArbeidsgiverTjeneste arbeidsgiverTjeneste,
                                      FamilieHendelseRepository familieHendelseRepository,
                                      InntektArbeidYtelseTjeneste iayTjeneste) {
        this.arbeidsgiverTjeneste = arbeidsgiverTjeneste;
        this.svangerskapspengerRepository = svangerskapspengerRepository;
        this.familieHendelseRepository = familieHendelseRepository;
        this.iayTjeneste = iayTjeneste;
    }

    public SvpTilretteleggingDto hentTilrettelegging(Long behandlingId) {
        SvpTilretteleggingDto dto = new SvpTilretteleggingDto();

        Optional<FamilieHendelseGrunnlagEntitet> familieHendelseGrunnlag = familieHendelseRepository.hentAggregatHvisEksisterer(behandlingId);
        if (familieHendelseGrunnlag.isEmpty()) {
            return dto;
        }

        Optional<TerminbekreftelseEntitet> terminbekreftelse = familieHendelseGrunnlag.get().getGjeldendeTerminbekreftelse();
        if (terminbekreftelse.isEmpty()) {
            throw SvangerskapsTjenesteFeil.FACTORY.kanIkkeFinneTerminbekreftelsePåSvangerskapspengerSøknad(behandlingId).toException();
        }
        dto.setTermindato(terminbekreftelse.get().getTermindato());
        familieHendelseGrunnlag.get().getGjeldendeVersjon().getFødselsdato().ifPresent(dto::setFødselsdato);

        Optional<SvpGrunnlagEntitet> svpGrunnlagOpt = svangerskapspengerRepository.hentGrunnlag(behandlingId);
        if (svpGrunnlagOpt.isEmpty()) {
            throw SvangerskapsTjenesteFeil.FACTORY.kanIkkeFinneSvangerskapspengerGrunnlagForBehandling(behandlingId).toException();
        }
        var arbeidsforholdInformasjon = iayTjeneste.hentGrunnlag(behandlingId).getArbeidsforholdInformasjon()
            .orElseThrow(() -> new IllegalStateException("Utviklerfeil: Fant ikke forventent arbeidsforholdinformasjon for behandling: " + behandlingId));

        var aktuelleTilretteleggingerUfiltrert = new TilretteleggingFilter(svpGrunnlagOpt.get()).getAktuelleTilretteleggingerUfiltrert().stream()
            .map(svpTilretteleggingEntitet -> mapTilrettelegging(svpTilretteleggingEntitet, arbeidsforholdInformasjon))
            .collect(Collectors.toList());
        dto.setArbeidsforholdListe(aktuelleTilretteleggingerUfiltrert);

        return dto;
    }

    private SvpArbeidsforholdDto mapTilrettelegging(SvpTilretteleggingEntitet svpTilrettelegging, ArbeidsforholdInformasjon arbeidsforholdInformasjon) {
        SvpArbeidsforholdDto dto = new SvpArbeidsforholdDto();
        dto.setTilretteleggingId(svpTilrettelegging.getId());
        dto.setTilretteleggingBehovFom(svpTilrettelegging.getBehovForTilretteleggingFom());
        dto.setTilretteleggingDatoer(utledTilretteleggingDatoer(svpTilrettelegging));
        dto.setOpplysningerOmRisiko(svpTilrettelegging.getOpplysningerOmRisikofaktorer().orElse(null));
        dto.setOpplysningerOmTilrettelegging(svpTilrettelegging.getOpplysningerOmTilretteleggingstiltak().orElse(null));
        dto.setBegrunnelse(svpTilrettelegging.getBegrunnelse().orElse(null));
        dto.setKopiertFraTidligereBehandling(svpTilrettelegging.getKopiertFraTidligereBehandling());
        dto.setMottattTidspunkt(svpTilrettelegging.getMottattTidspunkt());
        dto.setSkalBrukes(svpTilrettelegging.getSkalBrukes());
        svpTilrettelegging.getInternArbeidsforholdRef().ifPresent(ref -> {
            dto.setInternArbeidsforholdReferanse(ref.getReferanse());
            var arbeidsgiver = svpTilrettelegging.getArbeidsgiver()
                .orElseThrow(() -> new IllegalStateException("Utviklerfeil: Fant ikke forventent arbeidsgiver for tilrettelegging: " + svpTilrettelegging.getId()));
            dto.setEksternArbeidsforholdReferanse(arbeidsforholdInformasjon.finnEkstern(arbeidsgiver, ref).getReferanse());
        });
        if (svpTilrettelegging.getArbeidsgiver().isPresent()) {
            var arbeidsgiver = svpTilrettelegging.getArbeidsgiver().get();
            ArbeidsgiverOpplysninger arbeidsgiverOpplysninger = arbeidsgiverTjeneste.hent(svpTilrettelegging.getArbeidsgiver().get());
            dto.setArbeidsgiverNavn(arbeidsgiverOpplysninger.getNavn());
            dto.setArbeidsgiverIdent(arbeidsgiver.getIdentifikator());
            dto.setArbeidsgiverIdentVisning(arbeidsgiverOpplysninger.getIdentifikator());
        } else {
            dto.setArbeidsgiverNavn(svpTilrettelegging.getArbeidType().getNavn());
        }
        return dto;
    }

    private List<SvpTilretteleggingDatoDto> utledTilretteleggingDatoer(SvpTilretteleggingEntitet svpTilrettelegging) {
        return svpTilrettelegging.getTilretteleggingFOMListe().stream()
            .map(fom -> new SvpTilretteleggingDatoDto(fom.getFomDato(), fom.getType(), fom.getStillingsprosent(), fom.getOverstyrtUtbetalingsgrad()))
            .collect(Collectors.toList());
    }
}
