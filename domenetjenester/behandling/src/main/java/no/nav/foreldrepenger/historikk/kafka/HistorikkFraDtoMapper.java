package no.nav.foreldrepenger.historikk.kafka;

import java.util.Collection;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.aktør.NavBrukerKjønn;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkAktør;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.Historikkinnslag;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagDokumentLink;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRepository;
import no.nav.foreldrepenger.domene.typer.JournalpostId;
import no.nav.foreldrepenger.domene.typer.Saksnummer;
import no.nav.foreldrepenger.historikk.HistorikkInnslagTekstBuilder;
import no.nav.foreldrepenger.historikk.kafka.feil.HistorikkFeil;
import no.nav.historikk.kodeverk.BrukerKjønnEnum;
import no.nav.historikk.kodeverk.HistorikkAktørEnum;
import no.nav.historikk.kodeverk.HistorikkInnslagFeltTypeEnum;
import no.nav.historikk.v1.HistorikkInnslagDel;
import no.nav.historikk.v1.HistorikkInnslagDokumentLink;
import no.nav.historikk.v1.HistorikkInnslagFelt;
import no.nav.historikk.v1.HistorikkInnslagV1;

@ApplicationScoped
public class HistorikkFraDtoMapper {

    private static Map<BrukerKjønnEnum, NavBrukerKjønn> kjønnMap = new EnumMap<>(BrukerKjønnEnum.class);
    private static Map<HistorikkAktørEnum, HistorikkAktør> aktørMap = new EnumMap<>(HistorikkAktørEnum.class);

    static {
        kjønnMap.put(BrukerKjønnEnum.KVINNE, NavBrukerKjønn.KVINNE);
        kjønnMap.put(BrukerKjønnEnum.MANN, NavBrukerKjønn.MANN);

        aktørMap.put(HistorikkAktørEnum.SAKSBEHANDLER, HistorikkAktør.SAKSBEHANDLER);
        aktørMap.put(HistorikkAktørEnum.BESLUTTER, HistorikkAktør.BESLUTTER);
        aktørMap.put(HistorikkAktørEnum.VEDTAKSLØSNINGEN, HistorikkAktør.VEDTAKSLØSNINGEN);
        aktørMap.put(HistorikkAktørEnum.ARBEIDSGIVER, HistorikkAktør.ARBEIDSGIVER);
        aktørMap.put(HistorikkAktørEnum.SØKER, HistorikkAktør.SØKER);
    }

    private BehandlingRepository behandlingRepository;
    private FagsakRepository fagsakRepository;

    public HistorikkFraDtoMapper() {
        // CDI
    }

    @Inject
    public HistorikkFraDtoMapper(BehandlingRepository behandlingRepository, FagsakRepository fagsakRepository) {
        this.behandlingRepository = behandlingRepository;
        this.fagsakRepository = fagsakRepository;
    }

    Historikkinnslag opprettHistorikkInnslag(HistorikkInnslagV1 jsonHistorikk) {
        Behandling behandling = behandlingRepository.hentBehandling(jsonHistorikk.getBehandlingUuid());
        Fagsak fagsak = hentFagsak(jsonHistorikk, behandling);
        Historikkinnslag nyttHistorikkInnslag = new Historikkinnslag.Builder()
                .medAktør(mapAktør(jsonHistorikk))
                .medBehandlingId(behandling.getId())
                .medUuid(jsonHistorikk.getHistorikkUuid())
                .medFagsakId(fagsak.getId())
                .medKjoenn(mapKjønn(jsonHistorikk))
                .medType(HistorikkinnslagType.fraKode(jsonHistorikk.getHistorikkInnslagType()))
                .medHistorikkTid(jsonHistorikk.getOpprettetTidspunkt())
                .medOpprettetISystem(jsonHistorikk.getAvsender())
                .build();

        List<HistorikkinnslagDokumentLink> dokumentLenker = mapDokumentlenker(jsonHistorikk.getDokumentLinker(), nyttHistorikkInnslag);
        byggHistorikkDeler(jsonHistorikk.getHistorikkInnslagDeler(), nyttHistorikkInnslag);
        nyttHistorikkInnslag.setDokumentLinker(dokumentLenker);

        return nyttHistorikkInnslag;

    }

    private HistorikkAktør mapAktør(HistorikkInnslagV1 jsonHistorikk) {
        return aktørMap.get(jsonHistorikk.getHistorikkAktørType());
    }

    private NavBrukerKjønn mapKjønn(HistorikkInnslagV1 jsonHistorikk) {
        return jsonHistorikk.getBrukerKjoenn() != null ? kjønnMap.get(jsonHistorikk.getBrukerKjoenn()) : NavBrukerKjønn.UDEFINERT;
    }

    private Fagsak hentFagsak(HistorikkInnslagV1 jsonHistorikk, Behandling behandling) {
        return jsonHistorikk.getSaksnummer() != null
                ? fagsakRepository.hentSakGittSaksnummer(new Saksnummer(jsonHistorikk.getSaksnummer().getVerdi()))
                        .orElseThrow(IllegalStateException::new)
                : behandling.getFagsak();
    }

    private void byggHistorikkDeler(List<HistorikkInnslagDel> historikkinnslagDeler, Historikkinnslag nyttHistorikkInnslag) {
        if (HistorikkinnslagType.BREV_SENT.equals(nyttHistorikkInnslag.getType())) {
            mapBrevSendtDel(historikkinnslagDeler, nyttHistorikkInnslag);
            return;
        }
        if (!historikkinnslagDeler.isEmpty()) {
            throw new UnsupportedOperationException("Historikkinnslagdeler er kun implementert for sendte brev"); // Ikke triviell å implentere - vi
                                                                                                                  // har ikke behov for denne med det
                                                                                                                  // første
        }
    }

    private void mapBrevSendtDel(List<HistorikkInnslagDel> historikkinnslagDeler, Historikkinnslag nyttHistorikkInnslag) {
        String begrunnelse = historikkinnslagDeler.stream()
                .map(HistorikkInnslagDel::getHistorikkinnslagFelt)
                .flatMap(Collection::stream)
                .filter(felt -> HistorikkInnslagFeltTypeEnum.BEGRUNNELSE.equals(felt.getFeltType()))
                .map(HistorikkInnslagFelt::getTilVerdi)
                .findFirst().orElse("");
        new HistorikkInnslagTekstBuilder()
                .medHendelse(nyttHistorikkInnslag.getType())
                .medBegrunnelse(begrunnelse)
                .build(nyttHistorikkInnslag);
    }

    private List<HistorikkinnslagDokumentLink> mapDokumentlenker(List<HistorikkInnslagDokumentLink> dokumentLinker,
            Historikkinnslag historikkinnslag) {
        return dokumentLinker.stream().map(link -> mapDokumentlink(link, historikkinnslag)).collect(Collectors.toList());
    }

    private HistorikkinnslagDokumentLink mapDokumentlink(HistorikkInnslagDokumentLink dtoLink, Historikkinnslag historikkinnslag) {
        HistorikkinnslagDokumentLink.Builder builder = new HistorikkinnslagDokumentLink.Builder()
                .medLinkTekst(utledLinkTekst(dtoLink.getLinkTekst()))
                .medHistorikkinnslag(historikkinnslag)
                .medDokumentId(dtoLink.getDokumentId());
        if ((dtoLink.getJournalpostId() != null) && JournalpostId.erGyldig(dtoLink.getJournalpostId().getVerdi())) {
            builder.medJournalpostId(new JournalpostId(dtoLink.getJournalpostId().getVerdi()));
        } else {
            throw HistorikkFeil.FACTORY.ugyldigJournalpost(dtoLink.getJournalpostId() == null ? null : dtoLink.getJournalpostId().getVerdi())
                    .toException();// Burde aldri skje
        }
        return builder.build();
    }

    private static String utledLinkTekst(String tekst) {
        if ((tekst == null) || tekst.isEmpty() || tekst.isBlank()) {
            return "Ukjent brev";
        }
        return tekst;
    }
}
