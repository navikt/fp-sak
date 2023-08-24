package no.nav.foreldrepenger.behandlingslager.fagsak;


import java.lang.annotation.*;

@Inherited
@Documented
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface FagsakProsesstaskRekkefølge {

    /**
     * Hvorvidt task skal kjøres i sekvens (per gruppe, per fagsak) eller kan kjøres når som helst.
     * Dersom en task skal kjøres når som helst uten hensyn til andre tasks på fagsak, skal den ikke ha sideeffekter internt for behandlingen (det kan gå i beina på hverandre - race condition).
     * param gruppeSekvens true hvis må kjøres i rekkefølge, false hvis kan kjøres når som helst
     */
    boolean gruppeSekvens();
}
