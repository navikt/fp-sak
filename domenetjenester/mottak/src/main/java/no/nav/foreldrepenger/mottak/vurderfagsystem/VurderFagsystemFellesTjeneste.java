package no.nav.foreldrepenger.mottak.vurderfagsystem;

import static no.nav.foreldrepenger.behandling.BehandlendeFagsystem.BehandlendeSystem.MANUELL_VURDERING;
import static no.nav.foreldrepenger.behandling.BehandlendeFagsystem.BehandlendeSystem.VEDTAKSLØSNING;

import java.util.List;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlendeFagsystem;
import no.nav.foreldrepenger.behandling.FagsakTjeneste;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingTema;
import no.nav.foreldrepenger.behandlingslager.behandling.DokumentKategori;
import no.nav.foreldrepenger.behandlingslager.behandling.DokumentTypeId;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.domene.typer.Saksnummer;

@ApplicationScoped
public class VurderFagsystemFellesTjeneste {

    private FagsakTjeneste fagsakTjeneste;
    private VurderFagsystemFellesUtils fellesUtils;
    private Instance<VurderFagsystemTjeneste> vurderFagsystemTjenester;


    public VurderFagsystemFellesTjeneste(){
        //Injected normal scoped bean is now proxyable
    }

    @Inject
    public VurderFagsystemFellesTjeneste(FagsakTjeneste fagsakTjeneste,
                                         VurderFagsystemFellesUtils fellesUtils,
                                         @Any Instance<VurderFagsystemTjeneste> vurderFagsystemTjenester) {
        this.fagsakTjeneste = fagsakTjeneste;
        this.fellesUtils = fellesUtils;
        this.vurderFagsystemTjenester = vurderFagsystemTjenester;
    }

    public BehandlendeFagsystem vurderFagsystem(VurderFagsystem vurderFagsystem) {
        if (vurderFagsystem.getJournalpostId().isPresent()) {
            var journalpost = fagsakTjeneste.hentJournalpost(vurderFagsystem.getJournalpostId().get());
            if (journalpost.isPresent()) {
                return new BehandlendeFagsystem(VEDTAKSLØSNING, journalpost.get().getFagsak().getSaksnummer());
            }
        }
        // Endringssøknader og evt ettersendte vedlegg
        if (vurderFagsystem.getSaksnummer().isPresent()) {
            return vurderSøknadMedSaksnummer(vurderFagsystem.getSaksnummer().get());
        }
        var behandlingTema = vurderFagsystem.getBehandlingTema();
        var ytelseType = behandlingTema.getFagsakYtelseType();
        var alleBrukersFagsaker =  fagsakTjeneste.finnFagsakerForAktør(vurderFagsystem.getAktørId());

        if (DokumentTypeId.KLAGE_DOKUMENT.equals(vurderFagsystem.getDokumentTypeId()) || DokumentKategori.KLAGE_ELLER_ANKE.equals(vurderFagsystem.getDokumentKategori())) {
            return fellesUtils.vurderFagsystemKlageAnke(behandlingTema, alleBrukersFagsaker).orElse(new BehandlendeFagsystem(MANUELL_VURDERING));
        }

        if (BehandlingTema.UDEFINERT.equals(behandlingTema) || FagsakYtelseType.UDEFINERT.equals(ytelseType)) {
            return fellesUtils.standardUstrukturertDokumentVurdering(alleBrukersFagsaker).orElse(new BehandlendeFagsystem(MANUELL_VURDERING));
        }

        return håndterHenvendelse(vurderFagsystem, ytelseType, alleBrukersFagsaker);
    }

    private BehandlendeFagsystem vurderSøknadMedSaksnummer(Saksnummer saksnummer) {
        var sak = fagsakTjeneste.finnFagsakGittSaksnummer(saksnummer, false);
        if (sak.isEmpty() || sak.map(Fagsak::erStengt).orElse(Boolean.FALSE)) {
            return new BehandlendeFagsystem(MANUELL_VURDERING);
        }
        return new BehandlendeFagsystem(VEDTAKSLØSNING, saksnummer);
    }

    private BehandlendeFagsystem håndterHenvendelse(VurderFagsystem vurderFagsystem, FagsakYtelseType ytelseType, List<Fagsak> alleBrukersFagsaker) {
        var brukersSakerAvType = alleBrukersFagsaker.stream().filter(s -> ytelseType.equals(s.getYtelseType())).collect(Collectors.toList());

        var vurderFagsystemTjeneste = FagsakYtelseTypeRef.Lookup.find(vurderFagsystemTjenester, ytelseType)
            .orElseThrow(() -> new IllegalStateException("Ingen implementasjoner funnet for ytelse: " + ytelseType.getKode()));

        if (vurderFagsystem.erInntektsmelding()) {
            return vurderFagsystemTjeneste.vurderFagsystemInntektsmelding(vurderFagsystem, brukersSakerAvType);
        }

        if (vurderFagsystem.erStrukturertSøknad()) {
            return vurderFagsystemTjeneste.vurderFagsystemStrukturertSøknad(vurderFagsystem, brukersSakerAvType);
        }

        return vurderFagsystemTjeneste.vurderFagsystemUstrukturert(vurderFagsystem, brukersSakerAvType);
    }

    public BehandlendeFagsystem vurderFagsystemForKlageinstans(VurderFagsystem vurderFagsystem) {
        if (vurderFagsystem.getJournalpostId().isPresent()) {
            var journalpost = fagsakTjeneste.hentJournalpost(vurderFagsystem.getJournalpostId().get());
            if (journalpost.isPresent()) {
                return new BehandlendeFagsystem(VEDTAKSLØSNING, journalpost.get().getFagsak().getSaksnummer());
            }
        }
        // Endringssøknader og evt ettersendte vedlegg
        if (vurderFagsystem.getSaksnummer().isPresent()) {
            return vurderSøknadMedSaksnummer(vurderFagsystem.getSaksnummer().get());
        }
        var alleBrukersFagsaker =  fagsakTjeneste.finnFagsakerForAktør(vurderFagsystem.getAktørId());

        return fellesUtils.klageinstansUstrukturertDokumentVurdering(alleBrukersFagsaker).orElse(new BehandlendeFagsystem(MANUELL_VURDERING));
    }
}
