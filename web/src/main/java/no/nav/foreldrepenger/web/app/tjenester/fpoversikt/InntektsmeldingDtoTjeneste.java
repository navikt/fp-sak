package no.nav.foreldrepenger.web.app.tjenester.fpoversikt;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.behandling.MottattDokument;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.MottatteDokumentRepository;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektsmeldingTjeneste;
import no.nav.foreldrepenger.domene.iay.modell.Inntektsmelding;
import no.nav.foreldrepenger.domene.json.StandardJsonConfig;
import no.nav.foreldrepenger.domene.typer.Saksnummer;

@ApplicationScoped
class InntektsmeldingDtoTjeneste {

    private InntektsmeldingTjeneste inntektsmeldingTjeneste;
    private MottatteDokumentRepository mottatteDokumentRepository;

    @Inject
    InntektsmeldingDtoTjeneste(InntektsmeldingTjeneste inntektsmeldingTjeneste, MottatteDokumentRepository mottatteDokumentRepository) {
        this.inntektsmeldingTjeneste = inntektsmeldingTjeneste;
        this.mottatteDokumentRepository = mottatteDokumentRepository;
    }

    InntektsmeldingDtoTjeneste() {
        //CDI
    }


    public List<InntektsmeldingDto> hentInntektsmeldingerForSak(Saksnummer saksnummer) {
        return inntektsmeldingTjeneste.hentAlleInntektsmeldingerForFagsak(saksnummer)
            .stream()
            .map(this::map)
            .collect(Collectors.toList());
    }

    private InntektsmeldingDto map(Inntektsmelding inntektsmelding) {
        var arbeidsgiver = new Arbeidsgiver(inntektsmelding.getArbeidsgiver().getIdentifikator());
        var mottattTidspunkt = mottatteDokumentRepository.hentMottattDokument(inntektsmelding.getJournalpostId())
            .stream()
            .map(MottattDokument::getMottattTidspunkt)
            .min(LocalDateTime::compareTo)
            .orElseThrow(); //TODO

        var naturalytelser = inntektsmelding.getNaturalYtelser().stream().map(n -> new InntektsmeldingDto.NaturalYtelse(n.getPeriode().getFomDato(), n.getPeriode().getTomDato(),n.getBeloepPerMnd().getVerdi(), n.getType())).toList();
        var refusjon = inntektsmelding.getEndringerRefusjon().stream().map(r -> new InntektsmeldingDto.Refusjon(r.getRefusjonsbeløp().getVerdi(), r.getFom())).toList();

        return new InntektsmeldingDto(
            inntektsmelding.getInntektBeløp().getVerdi(),
            inntektsmelding.getRefusjonBeløpPerMnd() == null ? null : inntektsmelding.getRefusjonBeløpPerMnd().getVerdi(),
            arbeidsgiver,
            inntektsmelding.getJournalpostId(),
            inntektsmelding.getInnsendingstidspunkt(),
            mottattTidspunkt,
            inntektsmelding.getStartDatoPermisjon().orElse(null),
            naturalytelser,
            refusjon
        );
    }
}
