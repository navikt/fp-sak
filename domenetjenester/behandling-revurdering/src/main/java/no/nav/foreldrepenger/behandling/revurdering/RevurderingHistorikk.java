package no.nav.foreldrepenger.behandling.revurdering;

import static no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagLinjeBuilder.format;

import java.time.LocalDate;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;

import no.nav.foreldrepenger.behandlingslager.aktør.FødtBarnInfo;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkAktør;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.Historikkinnslag;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagLinjeBuilder;

/**
 * Lag historikk innslag ved revurdering.
 */
public class RevurderingHistorikk {
    private final HistorikkinnslagRepository historikkRepository;

    public RevurderingHistorikk(HistorikkinnslagRepository historikkRepository) {
        this.historikkRepository = historikkRepository;
    }

    public void opprettHistorikkinnslagOmRevurdering(Behandling behandling, BehandlingÅrsakType revurderingsÅrsak, boolean manueltOpprettet) {
        if (BehandlingÅrsakType.alleTekniskeÅrsaker().contains(revurderingsÅrsak)) {
            return;
        }
        var historikkAktør = manueltOpprettet ? HistorikkAktør.SAKSBEHANDLER : HistorikkAktør.VEDTAKSLØSNINGEN;
        var historikkinnslag = new Historikkinnslag.Builder()
            .medBehandlingId(behandling.getId())
            .medFagsakId(behandling.getFagsakId())
            .medAktør(historikkAktør)
            .medTittel("Revurdering er opprettet")
            .addLinje(revurderingsÅrsak.getNavn())
            .build();
        historikkRepository.lagre(historikkinnslag);
    }

    public void opprettHistorikkinnslagForFødsler(Behandling behandling, List<FødtBarnInfo> barnFødtIPeriode) {
        var fødselsdatoVerdi = getFødselsdatoVerdi(barnFødtIPeriode);
        var historikkinnslag = new Historikkinnslag.Builder()
            .medTittel("Opplysning om fødsel")
            .medFagsakId(behandling.getFagsakId())
            .medBehandlingId(behandling.getId())
            .medAktør(HistorikkAktør.VEDTAKSLØSNINGEN)
            .addLinje("Fødselsdato: " + fødselsdatoVerdi)
            .addLinje("Antall barn: " + barnFødtIPeriode.size())
            .build();
        historikkRepository.lagre(historikkinnslag);

    }

    private static String getFødselsdatoVerdi(List<FødtBarnInfo> barnFødtIPeriode) {
        if (barnFødtIPeriode.size() > 1) {
            SortedSet<LocalDate> fødselsdatoer = new TreeSet<>(
                    barnFødtIPeriode.stream().map(FødtBarnInfo::fødselsdato).collect(Collectors.toSet()));
            return fødselsdatoer.stream().map(HistorikkinnslagLinjeBuilder::format).collect(Collectors.joining(", "));
        }
        return format(barnFødtIPeriode.getFirst().fødselsdato());
    }

    public void opprettHistorikkinnslagForVenteFristRelaterteInnslag(Long behandlingId, Long fagsakId) {
        var historikkinnslag1 = new Historikkinnslag.Builder()
            .medFagsakId(fagsakId)
            .medBehandlingId(behandlingId)
            .medAktør(HistorikkAktør.VEDTAKSLØSNINGEN)
            .medTittel("Behandlingen er satt på vent")
            .addLinje("Søker eller den andre forelderen har en åpen behandling")
            .build();
        historikkRepository.lagre(historikkinnslag1);
    }

}
