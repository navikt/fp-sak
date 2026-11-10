package no.nav.foreldrepenger.behandlingslager.task;

import java.util.Optional;

import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakLås;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakLåsRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRelasjon;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRelasjonLåsegrunnlag;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRelasjonRepository;
import no.nav.foreldrepenger.behandlingslager.laas.FagsakRelasjonLås;
import no.nav.foreldrepenger.behandlingslager.laas.FagsakRelasjonLåsRepository;
import no.nav.foreldrepenger.domene.typer.Saksnummer;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskHandler;

/**
 * Task som utfører noe på en fagsakrelasjon, sikrer at relasjon låses fulgt av les behandling og skrivelås fagsak. Håper det unngår deadlock
 */
public abstract class FagsakRelasjonProsessTask implements ProsessTaskHandler {

    private FagsakRelasjonLåsRepository fagsakRelasjonLåsRepository;
    private FagsakLåsRepository fagsakLåsRepository;
    private FagsakRelasjonRepository fagsakRelasjonRepository;

    protected FagsakRelasjonProsessTask(FagsakLåsRepository fagsakLåsRepository, FagsakRelasjonLåsRepository relasjonLåsRepository, FagsakRelasjonRepository fagsakRelasjonRepository) {
        this.fagsakRelasjonLåsRepository = relasjonLåsRepository;
        this.fagsakLåsRepository = fagsakLåsRepository;
        this.fagsakRelasjonRepository = fagsakRelasjonRepository;
    }

    protected FagsakRelasjonProsessTask() {
        // for CDI proxy
    }

    @Override
    public void doTask(ProsessTaskData prosessTaskData) {
        var saksnummer = Saksnummer.fra(prosessTaskData.getSaksnummer());
        var grunnlag = fagsakRelasjonRepository.finnLåsegrunnlag(saksnummer);
        var fagsak1Lås = grunnlag.map(FagsakRelasjonLåsegrunnlag::saksnummerEn).map(fagsakLåsRepository::taLås);
        var fagsak2Lås = grunnlag.map(FagsakRelasjonLåsegrunnlag::saksnummerTo).map(fagsakLåsRepository::taLås);
        var reLås = grunnlag.map(fagsakRelasjonLåsRepository::taLåsForRelasjon).orElse(null);
        var fagsakRelasjon = grunnlag.flatMap(fagsakRelasjonRepository::finnFagsakRelasjonFor);

        prosesser(prosessTaskData, fagsakRelasjon, reLås, fagsak1Lås, fagsak2Lås);
    }

    protected abstract void prosesser(ProsessTaskData prosessTaskData, Optional<FagsakRelasjon> relasjon, FagsakRelasjonLås relasjonLås, Optional<FagsakLås> fagsak1Lås, Optional<FagsakLås> fagsak2Lås);

}
