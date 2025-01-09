package no.nav.foreldrepenger.domene.registerinnhenting.impl;

import static no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagLinjeBuilder.fraTilEquals;

import java.time.LocalDate;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkAktør;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.Historikkinnslag;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.skjermlenke.SkjermlenkeType;

@ApplicationScoped
public class RegisterinnhentingHistorikkinnslagTjeneste {

    private HistorikkinnslagRepository historikkinnslagRepository;

    RegisterinnhentingHistorikkinnslagTjeneste() {
        // for CDI proxy
    }

    @Inject
    public RegisterinnhentingHistorikkinnslagTjeneste(HistorikkinnslagRepository historikkinnslagRepository) {
        this.historikkinnslagRepository = historikkinnslagRepository;
    }

    public void opprettHistorikkinnslagForNyeRegisteropplysninger(Behandling behandling) {
        var historikkinnslag = new Historikkinnslag.Builder()
            .medTittel("Nye registeropplysninger")
            .medAktør(HistorikkAktør.VEDTAKSLØSNINGEN)
            .medFagsakId(behandling.getFagsakId())
            .medBehandlingId(behandling.getId())
            .addLinje("Saksbehandling starter på nytt")
            .build();
        historikkinnslagRepository.lagre(historikkinnslag);
    }

    public void opprettHistorikkinnslagForTilbakespoling(Behandling behandling, BehandlingStegType førSteg, BehandlingStegType etterSteg) {
        var historikkinnslag = new Historikkinnslag.Builder()
            .medTittel("Behandlingen er flyttet")
            .medAktør(HistorikkAktør.VEDTAKSLØSNINGEN)
            .medFagsakId(behandling.getFagsakId())
            .medBehandlingId(behandling.getId())
            .addLinje(String.format("Behandlingen er flyttet fra __%s__ tilbake til __%s__", førSteg.getNavn(), etterSteg.getNavn()))
            .build();
        historikkinnslagRepository.lagre(historikkinnslag);
    }

    public void opprettHistorikkinnslagForBehandlingMedNyeOpplysninger(Behandling behandling, BehandlingÅrsakType behandlingÅrsakType) {
        var historikkinnslag = new Historikkinnslag.Builder()
            .medTittel("Behandlingen oppdatert med nye opplysninger")
            .medAktør(HistorikkAktør.VEDTAKSLØSNINGEN)
            .medFagsakId(behandling.getFagsakId())
            .medBehandlingId(behandling.getId())
            .addLinje(behandlingÅrsakType.getNavn())
            .build();
        historikkinnslagRepository.lagre(historikkinnslag);
    }

    public void opprettHistorikkinnslagForEndretStartdatoEtterFødselshendelse(Behandling behandling, LocalDate endretFra, LocalDate endretTil) {
        var historikkinnslag = new Historikkinnslag.Builder()
            .medTittel(SkjermlenkeType.KONTROLL_AV_SAKSOPPLYSNINGER)
            .medAktør(HistorikkAktør.VEDTAKSLØSNINGEN)
            .medFagsakId(behandling.getFagsakId())
            .medBehandlingId(behandling.getId())
            .addLinje(fraTilEquals("Startdato for foreldrepengeperioden", endretFra, endretTil))
            .build();
        historikkinnslagRepository.lagre(historikkinnslag);
    }
}
