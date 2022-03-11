package no.nav.foreldrepenger.behandling.kabal;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.DokumentKategori;
import no.nav.foreldrepenger.behandlingslager.behandling.DokumentTypeId;
import no.nav.foreldrepenger.behandlingslager.behandling.MottattDokument;
import no.nav.foreldrepenger.behandlingslager.behandling.anke.AnkeRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.dokument.BehandlingDokumentBestiltEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.dokument.BehandlingDokumentEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.dokument.BehandlingDokumentRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageHjemmel;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageResultatEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageVurdertAv;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.MottatteDokumentRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.verge.VergeAggregat;
import no.nav.foreldrepenger.behandlingslager.behandling.verge.VergeOrganisasjonEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.verge.VergeRepository;
import no.nav.foreldrepenger.dokumentbestiller.DokumentMalType;
import no.nav.foreldrepenger.domene.person.PersoninfoAdapter;
import no.nav.foreldrepenger.domene.typer.PersonIdent;

@ApplicationScoped
public class KabalTjeneste {

    private AnkeRepository ankeRepository;
    private KlageRepository klageRepository;
    private BehandlingRepository behandlingRepository;
    private MottatteDokumentRepository mottatteDokumentRepository;
    private BehandlingDokumentRepository behandlingDokumentRepository;
    private VergeRepository vergeRepository;
    private PersoninfoAdapter personinfoAdapter;
    private KabalKlient kabalKlient;

    KabalTjeneste() {
        // for CDI proxy
    }

    @Inject
    public KabalTjeneste(PersoninfoAdapter personinfoAdapter,
                         KabalKlient kabalKlient,
                         BehandlingRepository behandlingRepository,
                         MottatteDokumentRepository mottatteDokumentRepository,
                         BehandlingDokumentRepository behandlingDokumentRepository,
                         VergeRepository vergeRepository,
                         AnkeRepository ankeRepository,
                         KlageRepository klageRepository) {
        this.personinfoAdapter = personinfoAdapter;
        this.ankeRepository = ankeRepository;
        this.klageRepository = klageRepository;
        this.behandlingRepository = behandlingRepository;
        this.mottatteDokumentRepository = mottatteDokumentRepository;
        this.behandlingDokumentRepository = behandlingDokumentRepository;
        this.vergeRepository = vergeRepository;
        this.kabalKlient = kabalKlient;
    }

    public void sentKlageTilKabal(Behandling behandling, KlageHjemmel hjemmel) {
        if (!BehandlingType.KLAGE.equals(behandling.getType())) {
            throw new IllegalArgumentException("Utviklerfeil: Prøver sende noe annet enn klage/anke til Kabal!");
        }
        var resultat = klageRepository.hentKlageVurderingResultat(behandling.getId(), KlageVurdertAv.NFP).orElseThrow();
        var brukHjemmel = Optional.ofNullable(hjemmel).or(() -> Optional.ofNullable(resultat.getKlageHjemmel()))
            .orElseThrow(() -> new IllegalArgumentException("Utviklerfeil: mangler hjemmel for klage"));
        var enhet = behandlingRepository.hentSisteYtelsesBehandlingForFagsakId(behandling.getFagsakId())
            .map(Behandling::getBehandlendeEnhet).orElseThrow();
        var klageMottattDato  = utledDokumentMottattDato(behandling);
        var klager = utledKlager(behandling, resultat.getKlageResultat());
        var request = TilKabalDto.klage(behandling, klager, enhet, finnDokumentReferanser(behandling, resultat.getKlageResultat()),
            klageMottattDato, klageMottattDato, List.of(brukHjemmel.getKabal()), resultat.getBegrunnelse());
        kabalKlient.sendTilKabal(request);
    }


    private TilKabalDto.Klager utledKlager(Behandling behandling, KlageResultatEntitet resultat) {
        var verge = vergeRepository.hentAggregat(behandling.getId())
            .or(() -> resultat.getPåKlagdBehandlingId().flatMap(b -> vergeRepository.hentAggregat(b)))
            .flatMap(VergeAggregat::getVerge)
            .map(v -> v.getVergeOrganisasjon().isPresent() ?
                new TilKabalDto.Part(TilKabalDto.PartsType.VIRKSOMHET, v.getVergeOrganisasjon().map(VergeOrganisasjonEntitet::getOrganisasjonsnummer).orElseThrow()) :
                new TilKabalDto.Part(TilKabalDto.PartsType.PERSON, personinfoAdapter.hentFnr(v.getBruker().getAktørId()).map(PersonIdent::getIdent).orElseThrow()))
            .map(p -> new TilKabalDto.Fullmektig(p, true));
        var klagerPart = new TilKabalDto.Part(TilKabalDto.PartsType.PERSON, personinfoAdapter.hentFnr(behandling.getAktørId()).map(PersonIdent::getIdent).orElseThrow());
        return new TilKabalDto.Klager(klagerPart, verge.orElse(null));

    }

    private LocalDate utledDokumentMottattDato(Behandling behandling) {
        return finnKlageAnkeDokument(behandling).stream()
            .map(MottattDokument::getMottattDato)
            .min(Comparator.naturalOrder())
            .orElseGet(() -> behandling.getOpprettetDato().toLocalDate());
    }

    private List<MottattDokument> finnKlageAnkeDokument(Behandling behandling) {
        return mottatteDokumentRepository.hentMottatteDokument(behandling.getId()).stream()
            .filter(d -> DokumentTypeId.KLAGE_DOKUMENT.equals(d.getDokumentType()) || DokumentKategori.KLAGE_ELLER_ANKE.equals(d.getDokumentKategori()))
            .toList();
    }

    private List<TilKabalDto.DokumentReferanse> finnDokumentReferanser(Behandling behandling, KlageResultatEntitet resultat) {
        List<TilKabalDto.DokumentReferanse> referanser = new ArrayList<>();
        behandlingDokumentRepository.hentHvisEksisterer(behandling.getId())
            .map(BehandlingDokumentEntitet::getBestilteDokumenter).orElse(List.of()).stream()
            .filter(d -> DokumentMalType.KLAGE_OVERSENDT.getKode().equals(d.getDokumentMalType()))
            .map(BehandlingDokumentBestiltEntitet::getJournalpostId)
            .filter(Objects::nonNull)
            .findFirst()
            .ifPresent(j -> referanser.add(new TilKabalDto.DokumentReferanse(j.getVerdi(), TilKabalDto.DokumentReferanseType.OVERSENDELSESBREV)));
        finnKlageAnkeDokument(behandling).stream()
            .map(MottattDokument::getJournalpostId)
            .forEach(j -> referanser.add(new TilKabalDto.DokumentReferanse(j.getVerdi(), TilKabalDto.DokumentReferanseType.BRUKERS_KLAGE)));
        resultat.getPåKlagdBehandlingId().flatMap(b -> behandlingDokumentRepository.hentHvisEksisterer(b))
            .map(BehandlingDokumentEntitet::getBestilteDokumenter).orElse(List.of()).stream()
            .filter(d -> d.getDokumentMalType() != null && DokumentMalType.erVedtaksBrev(DokumentMalType.fraKode(d.getDokumentMalType())))
            .map(BehandlingDokumentBestiltEntitet::getJournalpostId)
            .filter(Objects::nonNull)
            .findFirst()
            .ifPresent(j -> referanser.add(new TilKabalDto.DokumentReferanse(j.getVerdi(), TilKabalDto.DokumentReferanseType.OPPRINNELIG_VEDTAK)));
        resultat.getPåKlagdBehandlingId().map(b -> mottatteDokumentRepository.hentMottatteDokument(b)).orElse(List.of()).stream()
            .filter(MottattDokument::erSøknadsDokument)
            .map(MottattDokument::getJournalpostId)
            .distinct()
            .forEach(j -> referanser.add(new TilKabalDto.DokumentReferanse(j.getVerdi(), TilKabalDto.DokumentReferanseType.BRUKERS_SOEKNAD)));
        return referanser;
    }
}
