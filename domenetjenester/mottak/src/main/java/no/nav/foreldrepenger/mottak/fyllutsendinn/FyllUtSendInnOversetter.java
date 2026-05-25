package no.nav.foreldrepenger.mottak.fyllutsendinn;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.type.TypeReference;

import no.nav.foreldrepenger.behandlingslager.aktør.NavBrukerKjønn;
import no.nav.foreldrepenger.behandlingslager.aktør.PersoninfoKjønn;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.DokumentTypeId;
import no.nav.foreldrepenger.behandlingslager.behandling.MottattDokument;
import no.nav.foreldrepenger.behandlingslager.behandling.VariantFormat;
import no.nav.foreldrepenger.behandlingslager.behandling.dokument.MellomlagringEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.dokument.MellomlagringRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.dokument.MellomlagringType;
import no.nav.foreldrepenger.behandlingslager.behandling.søknad.ForeldreType;
import no.nav.foreldrepenger.behandlingslager.geografisk.Språkkode;
import no.nav.foreldrepenger.dokumentarkiv.ArkivDokument;
import no.nav.foreldrepenger.dokumentarkiv.ArkivJournalPost;
import no.nav.foreldrepenger.dokumentarkiv.DokumentArkivTjeneste;
import no.nav.foreldrepenger.dokumentarkiv.DokumentRespons;
import no.nav.foreldrepenger.domene.json.StandardJsonConfig;
import no.nav.foreldrepenger.domene.person.PersoninfoAdapter;
import no.nav.foreldrepenger.domene.typer.JournalpostId;
import no.nav.foreldrepenger.konfig.Environment;
import no.nav.foreldrepenger.mottak.fyllutsendinn.kilde.FormSubmission;
import no.nav.foreldrepenger.mottak.fyllutsendinn.kilde.Nav140507Data;
import no.nav.vedtak.mapper.json.DefaultJsonMapper;

@ApplicationScoped
public class FyllUtSendInnOversetter {

    private static final Logger LOG = LoggerFactory.getLogger(FyllUtSendInnOversetter.class);
    private static final Environment ENV = Environment.current();

    private MellomlagringRepository mellomlagringRepository;
    private DokumentArkivTjeneste dokumentArkivTjeneste;
    private PersoninfoAdapter personinfoAdapter;

    @Inject
    public FyllUtSendInnOversetter(DokumentArkivTjeneste dokumentArkivTjeneste,
                                   MellomlagringRepository mellomlagringRepository,
                                   PersoninfoAdapter personinfoAdapter) {
        this.mellomlagringRepository = mellomlagringRepository;
        this.dokumentArkivTjeneste = dokumentArkivTjeneste;
        this.personinfoAdapter = personinfoAdapter;
    }

    FyllUtSendInnOversetter() {
        // for CDI proxy
    }

    public void finnOgMellomlagreFyllUtSøknad(Behandling behandling, MottattDokument mottattDokument) {
        if (!DokumentTypeId.SØKNAD_ENGANGSSTØNAD_FØDSEL.equals(mottattDokument.getDokumentType()) || ENV.isProd()) {
            return;
        }
        try {
            var originalDokument = hentFyllUtJson(mottattDokument.getJournalpostId());
            if (originalDokument.isEmpty()) {
                return;
            }
            var esFødsel = StandardJsonConfig.fromJson(originalDokument.get(), new TypeReference<FormSubmission<Nav140507Data>>() {});
            // Skjemaspesifikke data
            var mellomlagringDto = EngangsstønadMapper.tilMellomlagreDto(esFødsel.data().data());
            // Data fra bruker, dokument og form-wrapper
            mellomlagringDto.setSpråkkode(Språkkode.finnForKodeverkEiersKode(esFødsel.language()));
            mellomlagringDto.setMottattDato(mottattDokument.getMottattDato());
            mellomlagringDto.setForeldreType(utledRolleES(behandling));
            var mellomlagring = MellomlagringEntitet.Builder.ny()
                .medBehandlingId(behandling.getId())
                .medType(MellomlagringType.PAPIRSØKNAD)
                .medInnhold(DefaultJsonMapper.toJson(mellomlagringDto))
                .build();
            mellomlagringRepository.lagreOgFlush(mellomlagring);
        } catch (Exception e) {
            LOG.warn("Feil ved oversetting og lagring av fyllut-json for journalpost {}", mottattDokument.getJournalpostId(), e);
        }
    }

    private Optional<String> hentFyllUtJson(JournalpostId journalpostId) {
        var journalpost = dokumentArkivTjeneste.hentJournalpostForSak(journalpostId);
        var dokumenter = new ArrayList<>(journalpost.map(ArkivJournalPost::getAndreDokument).orElseGet(List::of));
        journalpost.map(ArkivJournalPost::getHovedDokument).ifPresent(dokumenter::add);
        var originalDokumentId = dokumenter.stream()
            .filter(d -> d.getTilgjengeligSom().contains(VariantFormat.ORIGINAL))
            .findFirst().map(ArkivDokument::getDokumentId);
        return originalDokumentId.map(did -> dokumentArkivTjeneste.hentDokument(journalpostId, did))
            .map(DokumentRespons::innhold)
            .map(String::new)
            .map(String::trim)
            .filter(s -> s.startsWith("{"));
    }

    private ForeldreType utledRolleES(Behandling behandling) {
        var kjønn = personinfoAdapter.hentBrukerKjønnForAktør(behandling.getFagsakYtelseType(), behandling.getAktørId())
            .map(PersoninfoKjønn::kjønn)
            .filter(k -> !NavBrukerKjønn.UDEFINERT.equals(k))
            .orElse(NavBrukerKjønn.KVINNE);

        return NavBrukerKjønn.MANN.equals(kjønn) ? ForeldreType.FAR : ForeldreType.MOR;
    }

}
