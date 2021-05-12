package no.nav.foreldrepenger.skjæringstidspunkt;

import java.time.LocalDate;
import java.util.Map;

import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseType;
import no.nav.vedtak.util.env.Cluster;
import no.nav.vedtak.util.env.Environment;

/*
 * Klasse for styring av ikrafttredelese nytt regelverk for uttak
 * Metode for å gi ikrafttredelsesdato avhengig av miljø
 * Metode for å vurdere om en Familiehendelse skal vurderes etter nye eller gamle regler. Vil bli oppdatert
 */
public class Utsettelse2021 {

    private static final Cluster CURRENT_CLUSTER = Environment.current().getCluster();

    private static final Map<Cluster, LocalDate> DATO_MAP = Map.of(
        Cluster.PROD_FSS, LocalDate.of(2999,12,31),  // Nei, ikke endre denne før vi er klare
        Cluster.DEV_FSS, LocalDate.of(2021, 10, 1), // Ja, denne kan tilpasses til testbehov
        Cluster.LOCAL, LocalDate.of(2025,10,1) // Ja, endre denne når testklare
    );

    private static LocalDate DATO_LOKAL_TEST;

    public static LocalDate ikrafttredelseDato() {
        if (Cluster.LOCAL.equals(CURRENT_CLUSTER) && DATO_LOKAL_TEST != null) return DATO_LOKAL_TEST;
        return DATO_MAP.get(CURRENT_CLUSTER);
    }

    public static boolean skalBehandlesEtterNyeReglerUttak(FamilieHendelseGrunnlagEntitet familieHendelseGrunnlag) {
        if (familieHendelseGrunnlag == null) return false;
        var bekreftetFamilieHendelse = familieHendelseGrunnlag.getGjeldendeBekreftetVersjon()
            .filter(fh -> !FamilieHendelseType.TERMIN.equals(fh.getType()));
        if (bekreftetFamilieHendelse.isPresent()) {
            return bekreftetFamilieHendelse.map(FamilieHendelseEntitet::getSkjæringstidspunkt).filter(t -> !t.isBefore(ikrafttredelseDato())).isPresent();
        }
        var gjeldendeFH = familieHendelseGrunnlag.getGjeldendeVersjon();
        if (gjeldendeFH == null) return false;
        if (gjeldendeFH.getSkjæringstidspunkt().isBefore(Utsettelse2021.ikrafttredelseDato())) return false;
        if (!gjeldendeFH.getGjelderFødsel()) return LocalDate.now().isAfter(ikrafttredelseDato());
        return LocalDate.now().isAfter(ikrafttredelseDato().plusWeeks(2)); // Frist for registrering av fødsel i FREG
    }

    public static void setIkrafttredelseDatoEnhetstest(LocalDate testdato) {
        DATO_LOKAL_TEST = testdato;
    }

}
