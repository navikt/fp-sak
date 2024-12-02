package no.nav.foreldrepenger.behandling.revurdering;

import static no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagTekstBuilderFormater.formatDate;

import java.time.LocalDate;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;

import no.nav.foreldrepenger.behandlingslager.aktør.FødtBarnInfo;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkAktør;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.Historikkinnslag2;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.Historikkinnslag2Repository;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagTekstlinjeBuilder;

/**
 * Lag historikk innslag ved revurdering.
 */
public class RevurderingHistorikk {
    private final Historikkinnslag2Repository historikkRepository;

    public RevurderingHistorikk(Historikkinnslag2Repository historikkRepository) {
        this.historikkRepository = historikkRepository;
    }

    public void opprettHistorikkinnslagOmRevurdering(Behandling behandling, BehandlingÅrsakType revurderingsÅrsak, boolean manueltOpprettet) {
        if (BehandlingÅrsakType.alleTekniskeÅrsaker().contains(revurderingsÅrsak)) {
            return;
        }
        var historikkAktør = manueltOpprettet ? HistorikkAktør.SAKSBEHANDLER : HistorikkAktør.VEDTAKSLØSNINGEN;
        var historikkinnslag = new Historikkinnslag2.Builder()
            .medBehandlingId(behandling.getId())
            .medFagsakId(behandling.getFagsakId())
            .medAktør(historikkAktør)
            .medTittel("Revurdering opprettet")
            .addTekstlinje(revurderingsÅrsak.getNavn())
            .build();
        historikkRepository.lagre(historikkinnslag);
    }

    public void opprettHistorikkinnslagForFødsler(Behandling behandling, List<FødtBarnInfo> barnFødtIPeriode) {
        var fødselsdatoVerdi = getFødselsdatoVerdi(barnFødtIPeriode);
        var historikkinnslag = new Historikkinnslag2.Builder()
            .medTittel("Opplysning om fødsel")
            .medFagsakId(behandling.getFagsakId())
            .medBehandlingId(behandling.getId())
            .medAktør(HistorikkAktør.VEDTAKSLØSNINGEN)
            .addTekstlinje("Fødselsdato: " + fødselsdatoVerdi)
            .addTekstlinje("Antall barn: " + barnFødtIPeriode.size())
            .build();
        historikkRepository.lagre(historikkinnslag);

    }

    private static String getFødselsdatoVerdi(List<FødtBarnInfo> barnFødtIPeriode) {
        if (barnFødtIPeriode.size() > 1) {
            SortedSet<LocalDate> fødselsdatoer = new TreeSet<>(
                    barnFødtIPeriode.stream().map(FødtBarnInfo::fødselsdato).collect(Collectors.toSet()));
            return fødselsdatoer.stream().map(HistorikkinnslagTekstlinjeBuilder::formatDate).collect(Collectors.joining(", "));
        }
        return formatDate(barnFødtIPeriode.getFirst().fødselsdato());
    }

    public void opprettHistorikkinnslagForVenteFristRelaterteInnslag(Long behandlingId, Long fagsakId) {
        var historikkinnslag1 = new Historikkinnslag2.Builder()
            .medFagsakId(fagsakId)
            .medBehandlingId(behandlingId)
            .medAktør(HistorikkAktør.VEDTAKSLØSNINGEN)
            .medTittel("Behandlingen er satt på vent")
            .addTekstlinje("Søker eller den andre forelderen har en åpen behandling")
            .build();
        historikkRepository.lagre(historikkinnslag1);
    }

}
