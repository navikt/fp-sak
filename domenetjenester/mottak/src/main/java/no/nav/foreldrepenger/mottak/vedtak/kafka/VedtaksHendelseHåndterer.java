package no.nav.foreldrepenger.mottak.vedtak.kafka;

import java.io.IOException;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.control.ActivateRequestContext;
import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

import no.nav.abakus.iaygrunnlag.kodeverk.Fagsystem;
import no.nav.abakus.vedtak.ytelse.Ytelse;
import no.nav.abakus.vedtak.ytelse.v1.YtelseV1;
import no.nav.foreldrepenger.behandling.FagsakTjeneste;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.domene.tid.VirkedagUtil;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.mottak.json.JacksonJsonConfig;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.fpsak.tidsserie.StandardCombinators;
import no.nav.vedtak.feil.Feil;
import no.nav.vedtak.feil.FeilFactory;
import no.nav.vedtak.feil.LogLevel;
import no.nav.vedtak.feil.deklarasjon.DeklarerteFeil;
import no.nav.vedtak.feil.deklarasjon.TekniskFeil;

@ApplicationScoped
@ActivateRequestContext
@Transactional
public class VedtaksHendelseHåndterer {

    private static final Logger LOG = LoggerFactory.getLogger(VedtaksHendelseHåndterer.class);
    private static final ObjectMapper OBJECT_MAPPER = JacksonJsonConfig.getMapper();

    private FagsakTjeneste fagsakTjeneste;
    private BehandlingRepository behandlingRepository;
    private BeregningsresultatRepository tilkjentYtelseRepository;

    public VedtaksHendelseHåndterer() {
    }

    @Inject
    public VedtaksHendelseHåndterer(FagsakTjeneste fagsakTjeneste, BehandlingRepositoryProvider repositoryProvider) {
        this.fagsakTjeneste = fagsakTjeneste;
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.tilkjentYtelseRepository = repositoryProvider.getBeregningsresultatRepository();
    }

    void handleMessage(String key, String payload) {
        LOG.debug("Mottatt ytelse-vedtatt hendelse med key='{}', payload={}", key, payload);
        Ytelse mottattVedtak;
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()){
            mottattVedtak = OBJECT_MAPPER.readValue(payload, Ytelse.class);
            Validator validator = factory.getValidator();
            Set<ConstraintViolation<Ytelse>> violations = validator.validate(mottattVedtak);
            if (!violations.isEmpty()) {
                // Har feilet validering
                String allErrors = violations.stream().map(String::valueOf).collect(Collectors.joining("\\n"));
                LOG.info("Vedtatt-Ytelse valideringsfeil :: \n {}", allErrors);
                return;
            }
        } catch (IOException e) {
            YtelseFeil.FACTORY.parsingFeil(key, payload, e).log(LOG);
            return;
        }
        if (mottattVedtak == null)
            return;
        var ytelse = (YtelseV1) mottattVedtak;
        if (Fagsystem.FPSAK.equals(ytelse.getFagsystem())) {
            LOG.info("Vedtatt-Ytelse mottok eget vedtak i sak {}", ytelse.getSaksnummer());
            return;
        }
        LOG.info("Vedtatt-Ytelse mottok vedtak fra system {} saksnummer {} ytelse {}", ytelse.getFagsystem(), ytelse.getSaksnummer(), ytelse.getType());
        sjekkVedtakOverlapp(ytelse);
    }

    private void sjekkVedtakOverlapp(YtelseV1 ytelse) {
        List<Fagsak> fagsaker = fagsakTjeneste.finnFagsakerForAktør(new AktørId(ytelse.getAktør().getVerdi())).stream()
            .filter(f -> !FagsakYtelseType.ENGANGSTØNAD.equals(f.getYtelseType()))
            .filter(Fagsak::erÅpen) // OBS: fjern denne for å
            .collect(Collectors.toList());
        if (fagsaker.isEmpty())
            return;
        LOG.info("Vedtatt-Ytelse VL har disse sakene for bruker med vedtak {} - saker {}", ytelse.getType(), fagsaker);

        List<LocalDateSegment<Boolean>> ytelsesegments = ytelse.getAnvist().stream()
            .map(p -> new LocalDateSegment<>(VirkedagUtil.fomVirkedag(p.getPeriode().getFom()), VirkedagUtil.tomVirkedag(p.getPeriode().getTom()), Boolean.TRUE))
            .collect(Collectors.toList());

        List<LocalDateSegment<Boolean>> fpsegments = fagsaker.stream()
            .map(f -> behandlingRepository.finnSisteAvsluttedeIkkeHenlagteBehandling(f.getId()))
            .flatMap(Optional::stream)
            .map(b -> tilkjentYtelseRepository.hentBeregningsresultat(b.getId()))
            .flatMap(Optional::stream)
            .map(BeregningsresultatEntitet::getBeregningsresultatPerioder)
            .flatMap(Collection::stream)
            .filter(p -> p.getDagsats() > 0)
            .filter(p -> p.getBeregningsresultatPeriodeTom().isAfter(LocalDate.now().minusYears(1)))
            .map(p -> new LocalDateSegment<>(VirkedagUtil.fomVirkedag(p.getBeregningsresultatPeriodeFom()), VirkedagUtil.tomVirkedag(p.getBeregningsresultatPeriodeTom()), Boolean.TRUE))
            .collect(Collectors.toList());

        if (ytelsesegments.isEmpty() || fpsegments.isEmpty())
            return;

        var fpTidslinje = new LocalDateTimeline<>(fpsegments, StandardCombinators::alwaysTrueForMatch).compress();
        var ytelseTidslinje = new LocalDateTimeline<>(ytelsesegments, StandardCombinators::alwaysTrueForMatch).compress();

        if (!fpTidslinje.intersection(ytelseTidslinje).getDatoIntervaller().isEmpty())
            LOG.warn("Vedtatt-Ytelse KONTAKT PRODUKTEIER for overlapp mellom ytelse {} sak {} og VL-sakene {}", ytelse.getType(), ytelse.getSaksnummer(), fagsaker);
    }

    private interface YtelseFeil extends DeklarerteFeil {

        YtelseFeil FACTORY = FeilFactory.create(YtelseFeil.class);

        @TekniskFeil(feilkode = "FP-328773",
            feilmelding = "Vedtatt-Ytelse Feil under parsing av vedtak. key={%s} payload={%s}",
            logLevel = LogLevel.INFO)
        Feil parsingFeil(String key, String payload, IOException e);
    }

}
