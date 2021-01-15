package no.nav.foreldrepenger.mottak.lonnskomp.kafka;

import java.io.IOException;
import java.math.BigDecimal;
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

import no.nav.foreldrepenger.behandling.FagsakTjeneste;
import no.nav.foreldrepenger.behandlingslager.virksomhet.OrgNummer;
import no.nav.foreldrepenger.behandlingslager.ytelse.LønnskompensasjonVedtak;
import no.nav.foreldrepenger.domene.person.PersoninfoAdapter;
import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;
import no.nav.foreldrepenger.domene.typer.Beløp;
import no.nav.foreldrepenger.domene.typer.PersonIdent;
import no.nav.foreldrepenger.mottak.json.JacksonJsonConfig;
import no.nav.foreldrepenger.mottak.lonnskomp.domene.LønnskompensasjonRepository;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskRepository;

@ApplicationScoped
@ActivateRequestContext
@Transactional
public class LonnskompHendelseHåndterer {

    private static final Logger log = LoggerFactory.getLogger(LonnskompHendelseHåndterer.class);
    private final static ObjectMapper OBJECT_MAPPER = JacksonJsonConfig.getMapper();
    private LønnskompensasjonRepository repository;
    private ProsessTaskRepository prosessTaskRepository;
    private FagsakTjeneste fagsakTjeneste;
    private PersoninfoAdapter personinfoAdapter;


    public LonnskompHendelseHåndterer() {
    }

    @Inject
    public LonnskompHendelseHåndterer(LønnskompensasjonRepository repository,
                                      ProsessTaskRepository taskRepository,
                                      FagsakTjeneste fagsakTjeneste,
                                      PersoninfoAdapter personinfoAdapter) {
        this.repository = repository;
        this.prosessTaskRepository = taskRepository;
        this.fagsakTjeneste = fagsakTjeneste;
        this.personinfoAdapter = personinfoAdapter;
    }

    public void handleMessage(String key, String payload) {

        LønnskompensasjonVedtakMelding mottattVedtak;
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()){
            mottattVedtak = OBJECT_MAPPER.readValue(payload, LønnskompensasjonVedtakMelding.class);
            Validator validator = factory.getValidator();
            Set<ConstraintViolation<LønnskompensasjonVedtakMelding>> violations = validator.validate(mottattVedtak);
            if (!violations.isEmpty()) {
                // Har feilet validering
                String allErrors = violations.stream().map(String::valueOf).collect(Collectors.joining("\\n"));
                throw new IllegalArgumentException("Vedtatt-ytelse valideringsfeil :: \n " + allErrors);
            }
        } catch (IOException e) {
            throw LønnskompensasjonFeil.FACTORY.parsingFeil(key, payload, e).toException();
        }
        if (mottattVedtak != null) {
            var sakId = mottattVedtak.getSakId() != null ? mottattVedtak.getSakId() : mottattVedtak.getId();

            var vedtak = extractFrom(mottattVedtak, sakId);
            if (vedtak != null && fagsakTjeneste.finnesFagsakerForAktør(vedtak.getAktørId())) {
                var eksisterende = repository.hentSak(sakId, vedtak.getAktørId());
                if (repository.skalLagreVedtak(eksisterende.orElse(null), vedtak)) {
                    log.info("Lønnskomp lagrer sakId {}", sakId);
                    repository.lagre(vedtak);
                }
            }
        }
    }

    private LønnskompensasjonVedtak extractFrom(LønnskompensasjonVedtakMelding melding, String sakId) {
        if (melding.getTotalKompensasjon().compareTo(BigDecimal.ZERO) <= 0)
            return null;
        var vedtak = new LønnskompensasjonVedtak();
        vedtak.setAktørId(personinfoAdapter.hentAktørForFnr(new PersonIdent(melding.getFnr())).orElseThrow());
        //vedtak.setFnr(melding.getFnr());
        vedtak.setSakId(sakId);
        vedtak.setOrgNummer(new OrgNummer(melding.getBedriftNr()));
        vedtak.setPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(melding.getFom(), melding.getTom()));
        vedtak.setBeløp(new Beløp(melding.getTotalKompensasjon()));

        return vedtak;
    }

}
