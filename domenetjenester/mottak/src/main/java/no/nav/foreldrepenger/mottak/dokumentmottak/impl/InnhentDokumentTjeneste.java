package no.nav.foreldrepenger.mottak.dokumentmottak.impl;

import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.DokumentKategori;
import no.nav.foreldrepenger.behandlingslager.behandling.DokumentTypeId;
import no.nav.foreldrepenger.behandlingslager.behandling.MottattDokument;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.mottak.sakskompleks.KøKontroller;

@Dependent
public class InnhentDokumentTjeneste {

    private static Map<DokumentTypeId, DokumentGruppe> DOKUMENTTYPE_TIL_GRUPPE = new EnumMap<>(DokumentTypeId.class);
    static {
        // Søknad
        DokumentTypeId.getSøknadTyper().forEach(v -> DOKUMENTTYPE_TIL_GRUPPE.put(v, DokumentGruppe.SØKNAD));

        // Inntektsmelding
        DOKUMENTTYPE_TIL_GRUPPE.put(DokumentTypeId.INNTEKTSMELDING, DokumentGruppe.INNTEKTSMELDING);


        // Endringssøknad
        DokumentTypeId.getEndringSøknadTyper().forEach(v -> DOKUMENTTYPE_TIL_GRUPPE.put(v, DokumentGruppe.ENDRINGSSØKNAD));

        // Klage
        DOKUMENTTYPE_TIL_GRUPPE.put(DokumentTypeId.KLAGE_DOKUMENT, DokumentGruppe.KLAGE);
        DOKUMENTTYPE_TIL_GRUPPE.put(DokumentTypeId.KLAGE_ETTERSENDELSE, DokumentGruppe.KLAGE);
    }

    private static Map<DokumentKategori, DokumentGruppe> DOKUMENTKATEGORI_TIL_GRUPPE = new EnumMap<>(DokumentKategori.class);
    static {
        DOKUMENTKATEGORI_TIL_GRUPPE.put(DokumentKategori.SØKNAD, DokumentGruppe.SØKNAD);
        DOKUMENTKATEGORI_TIL_GRUPPE.put(DokumentKategori.KLAGE_ELLER_ANKE, DokumentGruppe.KLAGE);
    }

    private Instance<Dokumentmottaker> mottakere;

    private FagsakRepository fagsakRepository;

    private KøKontroller køKontroller;

    @Inject
    public InnhentDokumentTjeneste(BehandlingRepositoryProvider repositoryProvider,
                                   @Any Instance<Dokumentmottaker> mottakere,
                                   KøKontroller køKontroller) {
        this.fagsakRepository = repositoryProvider.getFagsakRepository();
        this.mottakere = mottakere;
        this.køKontroller = køKontroller;
    }

    public void utfør(MottattDokument mottattDokument, BehandlingÅrsakType behandlingÅrsakType) {
        var fagsak = fagsakRepository.finnEksaktFagsak(mottattDokument.getFagsakId());
        var dokumentTypeId = mottattDokument.getDokumentType();

        var dokumentGruppe = brukDokumentKategori(dokumentTypeId, mottattDokument.getDokumentKategori()) ?
            DOKUMENTKATEGORI_TIL_GRUPPE.getOrDefault(mottattDokument.getDokumentKategori(), DokumentGruppe.VEDLEGG) :
            DOKUMENTTYPE_TIL_GRUPPE.getOrDefault(dokumentTypeId, DokumentGruppe.VEDLEGG);

        var dokumentmottaker = finnMottaker(dokumentGruppe, fagsak.getYtelseType());
        if (dokumentmottaker.endringSomUtsetterStartdato(mottattDokument, fagsak)) {
            dokumentmottaker.mottaUtsettelseAvStartdato(mottattDokument, fagsak);
        } else if (skalMottasSomKøet(fagsak)) {
            dokumentmottaker.mottaDokumentForKøetBehandling(mottattDokument, fagsak, behandlingÅrsakType);
        } else {
            dokumentmottaker.mottaDokument(mottattDokument, fagsak, behandlingÅrsakType);
        }
    }

    public void opprettFraTidligereBehandling(Long behandlingId, MottattDokument mottattDokument, BehandlingÅrsakType behandlingÅrsakType) { //#SXX og #IXX
        Objects.requireNonNull(behandlingId, "behandlingId");
        var fagsak = fagsakRepository.finnEksaktFagsak(mottattDokument.getFagsakId());
        Dokumentmottaker dokumentmottaker;
        if (mottattDokument.erSøknadsDokument()) {
            dokumentmottaker = finnMottaker(DokumentGruppe.SØKNAD, fagsak.getYtelseType());
        } else if (mottattDokument.getDokumentType().erInntektsmelding()) {
            dokumentmottaker = finnMottaker(DokumentGruppe.INNTEKTSMELDING, fagsak.getYtelseType());
        } else {
            throw new IllegalArgumentException("Utvikler-feil: MottattDokument må være av type søknad eller inntektsmelding, var: " + mottattDokument.getDokumentType().getKode());
        }
        dokumentmottaker.opprettFraTidligereAvsluttetBehandling(fagsak, behandlingId, mottattDokument, behandlingÅrsakType, skalMottasSomKøet(fagsak));
    }

    private boolean skalMottasSomKøet(Fagsak fagsak) {
        return køKontroller.skalEvtNyBehandlingKøes(fagsak);
    }

    private boolean brukDokumentKategori(DokumentTypeId dokumentTypeId, DokumentKategori dokumentKategori) {
        return DokumentTypeId.UDEFINERT.equals(dokumentTypeId) || DokumentKategori.SØKNAD.equals(dokumentKategori) && dokumentTypeId.erAnnenDokType();
    }

    private Dokumentmottaker finnMottaker(DokumentGruppe dokumentGruppe, FagsakYtelseType fagsakYtelseType) {
        var selected = mottakere.select(new DokumentGruppeRef.DokumentGruppeRefLiteral(dokumentGruppe));

        if (selected.isAmbiguous()) {
            selected = selected.select(new FagsakYtelseTypeRef.FagsakYtelseTypeRefLiteral(fagsakYtelseType));
        }

        if (selected.isAmbiguous()) {
            throw new IllegalArgumentException("Mer enn en implementasjon funnet for DokumentGruppe=" + dokumentGruppe + ", FagsakYtelseType=" + fagsakYtelseType);
        }
        if (selected.isUnsatisfied()) {
            throw new IllegalArgumentException("Ingen implementasjoner funnet for DokumentGruppe=" + dokumentGruppe + ", FagsakYtelseType=" + fagsakYtelseType);
        }
        var minInstans = selected.get();
        if (minInstans.getClass().isAnnotationPresent(Dependent.class)) {
            throw new IllegalStateException("Kan ikke ha @Dependent scope bean ved Instance lookup dersom en ikke også håndtere lifecycle selv: " + minInstans.getClass());
        }
        return minInstans;
    }
}
