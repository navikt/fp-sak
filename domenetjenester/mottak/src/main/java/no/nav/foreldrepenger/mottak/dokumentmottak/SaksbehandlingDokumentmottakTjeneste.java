package no.nav.foreldrepenger.mottak.dokumentmottak;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.DokumentKategori;
import no.nav.foreldrepenger.behandlingslager.behandling.MottattDokument;
import no.nav.foreldrepenger.domene.typer.JournalpostId;
import no.nav.foreldrepenger.mottak.dokumentmottak.impl.HåndterMottattDokumentTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskRepository;

@ApplicationScoped
public class SaksbehandlingDokumentmottakTjeneste {

    private static final Logger log = org.slf4j.LoggerFactory.getLogger(SaksbehandlingDokumentmottakTjeneste.class);

    private ProsessTaskRepository prosessTaskRepository;
    private MottatteDokumentTjeneste mottatteDokumentTjeneste;

    public SaksbehandlingDokumentmottakTjeneste() {
        //for CDI, jaja
    }

    @Inject
    public SaksbehandlingDokumentmottakTjeneste(ProsessTaskRepository prosessTaskRepository,
                                                MottatteDokumentTjeneste mottatteDokumentTjeneste) {
        this.prosessTaskRepository = prosessTaskRepository;
        this.mottatteDokumentTjeneste = mottatteDokumentTjeneste;
    }

    public void dokumentAnkommet(InngåendeSaksdokument saksdokument) {
        boolean erElektroniskSøknad = saksdokument.getPayloadXml() != null;

        MottattDokument.Builder builder = new MottattDokument.Builder()
            .medDokumentType(saksdokument.getDokumentTypeId())
            .medDokumentKategori(saksdokument.getDokumentKategori() != null ? saksdokument.getDokumentKategori() : DokumentKategori.UDEFINERT)
            .medMottattDato(LocalDate.parse(saksdokument.getForsendelseMottatt().toString()))
            .medXmlPayload(saksdokument.getPayloadXml())
            .medElektroniskRegistrert(erElektroniskSøknad)
            .medFagsakId(saksdokument.getFagsakId())
            .medJournalFørendeEnhet(saksdokument.getJournalEnhet());

        if (saksdokument.getForsendelseMottattTidspunkt() == null) {
            builder.medMottattTidspunkt(LocalDateTime.now());
        } else {
            builder.medMottattTidspunkt(LocalDateTime.parse(saksdokument.getForsendelseMottattTidspunkt().toString()));
        }
        if (saksdokument.getKanalreferanse() != null) {
            builder.medKanalreferanse(saksdokument.getKanalreferanse());
        }

        if (saksdokument.getJournalpostId() != null) {
            builder.medJournalPostId(new JournalpostId(saksdokument.getJournalpostId().getVerdi()));
        }
        if (saksdokument.getForsendelseId() != null) {
            builder.medForsendelseId(UUID.fromString(saksdokument.getForsendelseId().toString()));
        }
        MottattDokument mottattDokument = builder.build();

        Long mottattDokumentId = mottatteDokumentTjeneste.lagreMottattDokumentPåFagsak(mottattDokument);

        ProsessTaskData prosessTaskData = new ProsessTaskData(HåndterMottattDokumentTask.TASKTYPE);
        prosessTaskData.setFagsakId(saksdokument.getFagsakId());
        prosessTaskData.setProperty(HåndterMottattDokumentTask.MOTTATT_DOKUMENT_ID_KEY, mottattDokumentId.toString());
        settÅrsakHvisDefinert(saksdokument.getBehandlingÅrsakType(), prosessTaskData);
        prosessTaskData.setCallIdFraEksisterende();
        prosessTaskRepository.lagre(prosessTaskData);
    }

    public void opprettFraTidligereBehandling(MottattDokument mottattDokument, Behandling behandling, BehandlingÅrsakType behandlingÅrsakType) {
        log.info("Oppretter håndtermottattdokumenttask fra tidligere behandling {} fagsak {} dokument {}", behandling.getId(), behandling.getFagsakId(), mottattDokument.getId());
        ProsessTaskData prosessTaskData = new ProsessTaskData(HåndterMottattDokumentTask.TASKTYPE);
        prosessTaskData.setBehandling(behandling.getFagsakId(), behandling.getId(), behandling.getAktørId().getId());
        prosessTaskData.setProperty(HåndterMottattDokumentTask.MOTTATT_DOKUMENT_ID_KEY, mottattDokument.getId().toString());
        settÅrsakHvisDefinert(behandlingÅrsakType, prosessTaskData);
        prosessTaskData.setCallIdFraEksisterende();
        prosessTaskRepository.lagre(prosessTaskData);
    }

    public void mottaUbehandletSøknad(MottattDokument mottattDokument, BehandlingÅrsakType behandlingÅrsakType) {
        ProsessTaskData prosessTaskData = new ProsessTaskData(HåndterMottattDokumentTask.TASKTYPE);

        prosessTaskData.setFagsakId(mottattDokument.getFagsakId());
        prosessTaskData.setProperty(HåndterMottattDokumentTask.MOTTATT_DOKUMENT_ID_KEY, mottattDokument.getId().toString());
        settÅrsakHvisDefinert(behandlingÅrsakType, prosessTaskData);
        prosessTaskData.setCallIdFraEksisterende();
        prosessTaskRepository.lagre(prosessTaskData);
    }

    private void settÅrsakHvisDefinert(BehandlingÅrsakType behandlingÅrsakType, ProsessTaskData prosessTaskData) {
        if (behandlingÅrsakType !=null && !BehandlingÅrsakType.UDEFINERT.equals(behandlingÅrsakType)) {
            prosessTaskData.setProperty(HåndterMottattDokumentTask.BEHANDLING_ÅRSAK_TYPE_KEY, behandlingÅrsakType.getKode());
        }
    }
}
