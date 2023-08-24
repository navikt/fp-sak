package no.nav.foreldrepenger.behandlingslager;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.søknad.SøknadEntitet;
import no.nav.foreldrepenger.behandlingslager.diff.TraverseGraph;
import no.nav.foreldrepenger.behandlingslager.diff.TraverseGraphConfig;
import no.nav.foreldrepenger.behandlingslager.diff.TraverseJpaEntityGraphConfig;
import no.nav.foreldrepenger.behandlingslager.fagsak.Dekningsgrad;
import no.nav.foreldrepenger.behandlingslager.kodeverk.Kodeverdi;
import no.nav.foreldrepenger.behandlingslager.uttak.Utbetalingsgrad;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.SamtidigUttaksprosent;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.Trekkdager;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.behandlingslager.virksomhet.OrgNummer;
import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;
import no.nav.foreldrepenger.domene.tid.ÅpenDatoIntervallEntitet;
import no.nav.foreldrepenger.domene.typer.*;

import java.util.function.Function;

public final class TraverseEntityGraphFactory {
    private TraverseEntityGraphFactory() {
    }

    public static TraverseGraph build(boolean medChangedTrackedOnly, Class<?>... leafClasses) {
        return build(medChangedTrackedOnly, TraverseGraphConfig.NO_FILTER, leafClasses);
    }

    public static TraverseGraph build(boolean medChangedTrackedOnly, Function<Object, Boolean> inclusionFilter, Class<?>... leafClasses) {

        /* default oppsett for behandlingslager. */

        var config = new TraverseJpaEntityGraphConfig();
        config.setIgnoreNulls(true);
        config.setOnlyCheckTrackedFields(medChangedTrackedOnly);
        config.addRootClasses(Behandling.class, SøknadEntitet.class);
        config.setInclusionFilter(inclusionFilter);

        config.addLeafClasses(AktørId.class);
        config.addLeafClasses(Saksnummer.class);
        config.addLeafClasses(JournalpostId.class);
        config.addLeafClasses(PersonIdent.class);
        config.addLeafClasses(OrgNummer.class);
        config.addLeafClasses(EksternArbeidsforholdRef.class);
        config.addLeafClasses(InternArbeidsforholdRef.class);
        config.addLeafClasses(Stillingsprosent.class);
        config.addLeafClasses(Arbeidsgiver.class);
        config.addLeafClasses(Beløp.class);
        config.addLeafClasses(Utbetalingsgrad.class);
        config.addLeafClasses(SamtidigUttaksprosent.class);
        config.addLeafClasses(Trekkdager.class);
        config.addLeafClasses(Dekningsgrad.class);

        config.addLeafClasses(DatoIntervallEntitet.class, ÅpenDatoIntervallEntitet.class);
        config.addLeafClasses(Kodeverdi.class);

        config.addLeafClasses(leafClasses);
        return new TraverseGraph(config);
    }

    public static TraverseGraph build() {
        return build(false);
    }
}
