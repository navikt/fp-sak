package no.nav.foreldrepenger.familiehendelse.historikk;

import jakarta.enterprise.context.ApplicationScoped;

import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.Historikkinnslag;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagLinjeBuilder;
import no.nav.foreldrepenger.familiehendelse.modell.FødselStatus;
import no.nav.foreldrepenger.familiehendelse.rest.BarnInfoProvider;

import java.util.Objects;

import static no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagLinjeBuilder.fraTilEquals;
import static no.nav.foreldrepenger.familiehendelse.modell.FødselStatus.safeGet;

@ApplicationScoped
public class FødselHistorikkTjeneste {

    private FødselHistorikkTjeneste() {
    }

    public static void lagHistorikkForBarn(Historikkinnslag.Builder historikkinnslag,
                                           FamilieHendelseGrunnlagEntitet grunnlag,
                                           BarnInfoProvider barnInfo) {
        var oppdatertFødselStatus = barnInfo.getBarn().stream().map(FødselStatus::new).toList();
        var gjeldendeFødselStatus = grunnlag.getGjeldendeBarna().stream().map(FødselStatus::new).toList();

        if (!Objects.equals(oppdatertFødselStatus.size(), grunnlag.getGjeldendeAntallBarn())) {
            historikkinnslag.addLinje(
                new HistorikkinnslagLinjeBuilder().fraTil("Antall barn", grunnlag.getGjeldendeAntallBarn(), oppdatertFødselStatus.size()));
        } else {
            historikkinnslag.addLinje(
                new HistorikkinnslagLinjeBuilder().bold("Antall barn").tekst("som brukes i behandlingen:").bold(oppdatertFødselStatus.size()));
        }

        if (!oppdatertFødselStatus.equals(gjeldendeFødselStatus)) {
            var lengsteListeStørrelse = Math.max(oppdatertFødselStatus.size(), gjeldendeFødselStatus.size());
            for (int i = 0; i < lengsteListeStørrelse; i++) {
                var til = safeGet(oppdatertFødselStatus, i).map(FødselStatus::formaterLevetid).orElse(null);
                var fra = safeGet(gjeldendeFødselStatus, i).map(FødselStatus::formaterLevetid).orElse(null);
                var barn = lengsteListeStørrelse > 1 ? "Barn " + (i + 1) : "Barn";
                historikkinnslag.addLinje(fraTilEquals(barn, fra, til));
            }
        }
    }
}
