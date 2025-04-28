package no.nav.foreldrepenger.web.app.tjenester.forvaltning;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.fpinntektsmelding.FpInntektsmeldingTjeneste;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.foreldrepenger.domene.iay.modell.InntektsmeldingAggregat;
import no.nav.foreldrepenger.domene.typer.Beløp;
import no.nav.foreldrepenger.domene.typer.JournalpostId;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskHandler;
import no.nav.vedtak.mapper.json.DefaultJsonMapper;

/*
 * Overstyring av inntektsmeldingtask. Gjøres i egen task for å sikre systemkontekst
 */
@Dependent
@ProsessTask(value = "fpinntektsmelding.overstyr.inntektsmelding", prioritet = 2)
@FagsakProsesstaskRekkefølge(gruppeSekvens = false)
class OverstyrInntektsmeldingTask implements ProsessTaskHandler {

    private final InntektArbeidYtelseTjeneste iayTjeneste;
    private final FpInntektsmeldingTjeneste fpInntektsmeldingTjeneste;
    private final BehandlingRepository behandlingRepository;

    @Inject
    public OverstyrInntektsmeldingTask(InntektArbeidYtelseTjeneste iayTjeneste,
                                       FpInntektsmeldingTjeneste fpInntektsmeldingTjeneste,
                                       BehandlingRepository behandlingRepository) {
        this.iayTjeneste = iayTjeneste;
        this.fpInntektsmeldingTjeneste = fpInntektsmeldingTjeneste;
        this.behandlingRepository = behandlingRepository;
    }

    @Override
    public void doTask(ProsessTaskData prosessTaskData) {
        var json = prosessTaskData.getPayloadAsString();
        var endringIInntektsmelding = DefaultJsonMapper.fromJson(json, InntektsmeldingEndring.class);

        var jpId = new JournalpostId(endringIInntektsmelding.journalpostId());
        var ref = BehandlingReferanse.fra(behandlingRepository.hentBehandling(endringIInntektsmelding.behandlingId()));

        var inntektsmeldingSomSkalOverstyres = iayTjeneste.finnGrunnlag(endringIInntektsmelding.behandlingId())
            .flatMap(InntektArbeidYtelseGrunnlag::getInntektsmeldinger)
            .map(InntektsmeldingAggregat::getAlleInntektsmeldinger)
            .orElse(List.of())
            .stream()
            .filter(im -> im.getJournalpostId().equals(jpId))
            .findFirst()
            .orElseThrow();

        Map<LocalDate, Beløp> refusjonsendringMap = endringIInntektsmelding.getRefusjonsendringer()
            .stream()
            .collect(Collectors.toMap(InntektsmeldingEndring.Refusjonsendring::fom, e -> Beløp.fra(BigDecimal.valueOf(e.beløp()))));

        fpInntektsmeldingTjeneste.overstyrInntektsmelding(inntektsmeldingSomSkalOverstyres, endringIInntektsmelding.opphørdato(), refusjonsendringMap, endringIInntektsmelding.saksbehandlerIdent(), ref);
    }

    public record InntektsmeldingEndring(String journalpostId, Long behandlingId, LocalDate opphørdato, String saksbehandlerIdent, List<Refusjonsendring> refusjonsendringer) {
        public List<Refusjonsendring> getRefusjonsendringer(){
            return refusjonsendringer == null ? List.of() : refusjonsendringer;
        }
        public record Refusjonsendring(LocalDate fom, Long beløp){}
    }
}
