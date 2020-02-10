package no.nav.foreldrepenger.mottak.publiserer.task;


import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandlingslager.behandling.MottattDokument;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRepository;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektsmeldingTjeneste;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.InntektsmeldingInnsendingsårsak;
import no.nav.foreldrepenger.mottak.dokumentmottak.MottatteDokumentTjeneste;
import no.nav.foreldrepenger.mottak.dokumentmottak.impl.HåndterMottattDokumentTaskProperties;
import no.nav.foreldrepenger.mottak.publiserer.producer.DialogHendelseProducer;
import no.nav.foreldrepenger.mottak.publiserer.producer.DialogJsonConfig;
import no.nav.foreldrepenger.mottak.publiserer.producer.PubliserPersistertDokumentHendelseFeil;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskHandler;
import no.nav.vedtak.hendelser.inntektsmelding.v1.InntektsmeldingV1;

@ApplicationScoped
@FagsakProsesstaskRekkefølge(gruppeSekvens = true)
@ProsessTask(PubliserPersistertDokumentHendelseTask.TASKTYPE)
public class PubliserPersistertDokumentHendelseTask implements ProsessTaskHandler {

    public static final String TASKTYPE = "mottak.publiserPersistertDokument";
    public static final String MOTTATT_DOKUMENT_ID_KEY = "mottattDokumentId";

    private static final Logger log = LoggerFactory.getLogger(PubliserPersistertDokumentHendelseTask.class);

    private FagsakRepository fagsakRepository;
    private MottatteDokumentTjeneste mottatteDokumentTjeneste;
    private InntektsmeldingTjeneste inntektsmeldingTjeneste;
    private DialogHendelseProducer producer;

    public PubliserPersistertDokumentHendelseTask() {
        // CDI krav
    }

    @Inject
    public PubliserPersistertDokumentHendelseTask(FagsakRepository fagsakRepository,  // NOSONAR
                                                      MottatteDokumentTjeneste mottatteDokumentTjeneste,
                                                      InntektsmeldingTjeneste inntektsmeldingTjeneste,
                                                      DialogHendelseProducer producer) { // NOSONAR
        this.fagsakRepository = fagsakRepository;
        this.mottatteDokumentTjeneste = mottatteDokumentTjeneste;
        this.inntektsmeldingTjeneste = inntektsmeldingTjeneste;
        this.producer = producer;
    }

    @Override
    public void doTask(ProsessTaskData data) {
        Fagsak fagsak = fagsakRepository.finnEksaktFagsak(data.getFagsakId());
        Optional<MottattDokument> dokumentOptional = mottatteDokumentTjeneste.hentMottattDokument(Long.valueOf(data.getPropertyValue(HåndterMottattDokumentTaskProperties.MOTTATT_DOKUMENT_ID_KEY)));
        dokumentOptional.ifPresent(dokument -> {
            inntektsmeldingTjeneste.hentInntektsMeldingFor(data.getBehandlingId(), dokument.getJournalpostId()).ifPresent(inntektsmelding -> {
                log.info("[DIALOG-HENDELSE] Inntektsmelding persistert : {}", inntektsmelding.getKanalreferanse());
                InntektsmeldingInnsendingsårsak årsak = inntektsmelding.getInntektsmeldingInnsendingsårsak();
                if (årsak == null || InntektsmeldingInnsendingsårsak.UDEFINERT.equals(årsak)) {
                    årsak = InntektsmeldingInnsendingsårsak.NY;
                }
                final InntektsmeldingV1 hendelse = new InntektsmeldingV1.Builder()
                    .medAktørId(data.getAktørId())
                    .medArbeidsgiverId(inntektsmelding.getArbeidsgiver().getIdentifikator())
                    .medInnsendingsÅrsak(årsak.getKode())
                    .medInnsendingsTidspunkt(inntektsmelding.getInnsendingstidspunkt())
                    .medJournalpostId(dokument.getJournalpostId().getVerdi())
                    .medReferanseId(inntektsmelding.getKanalreferanse())
                    .medStartDato(inntektsmelding.getStartDatoPermisjon().orElse(null))
                    .medSaksnummer(fagsak.getSaksnummer().getVerdi())
                    .build();
                producer.sendJsonMedNøkkel(inntektsmelding.getKanalreferanse(), DialogJsonConfig.toJson(hendelse, PubliserPersistertDokumentHendelseFeil.FEILFACTORY::kanIkkeSerialisere));
            });
        });
    }
}
