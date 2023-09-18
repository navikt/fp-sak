package no.nav.foreldrepenger.web.app.tjenester.fpoversikt;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.behandling.MottattDokument;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.MottatteDokumentRepository;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektsmeldingTjeneste;
import no.nav.foreldrepenger.domene.iay.modell.Inntektsmelding;
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


    public Set<InntektsmeldingDto> hentInntektsmeldingerForSak(Saksnummer saksnummer) {
        return inntektsmeldingTjeneste.hentAlleInntektsmeldingerForFagsak(saksnummer)
            .stream()
            .map(this::map)
            .collect(Collectors.toSet());
    }

    private InntektsmeldingDto map(Inntektsmelding inntektsmelding) {
        var arbeidsgiver = new Arbeidsgiver(inntektsmelding.getArbeidsgiver().getIdentifikator());
        var mottattTidspunkt = mottatteDokumentRepository.hentMottattDokument(inntektsmelding.getJournalpostId())
            .stream()
            .map(MottattDokument::getMottattTidspunkt)
            .min(LocalDateTime::compareTo)
            .orElseThrow();
        return new InntektsmeldingDto(inntektsmelding.getJournalpostId(),
            arbeidsgiver, inntektsmelding.getInnsendingstidspunkt(), inntektsmelding.getInntektBeløp().getVerdi(), mottattTidspunkt);
    }
}
