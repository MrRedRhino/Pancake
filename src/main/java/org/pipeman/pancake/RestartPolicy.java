package org.pipeman.pancake;

/**
 * @param maxRetries                How often to restart the server after it stopped. Resets after a successful boot. -1 = Infinite
 * @param failedBootMinUptime       Minimum uptime to consider a boot successful
 * @param failedBootNoNetworkAccess Whether network access is required to consider a boot successful
 */
public record RestartPolicy(int maxRetries, int failedBootMinUptime, boolean failedBootNoNetworkAccess) {

}
