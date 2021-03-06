/**
 * Copyright (c) 2000-2011 Liferay, Inc. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 */

package com.liferay.portal.notifications;

import com.liferay.portal.kernel.notifications.Channel;
import com.liferay.portal.kernel.notifications.ChannelException;
import com.liferay.portal.kernel.notifications.ChannelHub;
import com.liferay.portal.kernel.notifications.ChannelListener;
import com.liferay.portal.kernel.notifications.NotificationEvent;
import com.liferay.portal.kernel.notifications.UnknownChannelException;
import com.liferay.portal.model.CompanyConstants;
import com.liferay.portal.service.UserNotificationEventLocalServiceUtil;
import com.liferay.portal.util.PropsValues;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * @author Edward Han
 * @author Brian Wing Shun Chan
 * @author Shuyang Zhou
 */
public class ChannelHubImpl implements ChannelHub {

	public void cleanUp() throws ChannelException {
		for (Channel channel : _channels.values()) {
			channel.cleanUp();
		}
	}

	public void cleanUp(long userId) throws ChannelException {
		Channel channel = getChannel(userId);

		channel.cleanUp();
	}

	public ChannelHub clone(long companyId) {
		ChannelHubImpl channelHubImpl = new ChannelHubImpl();

		channelHubImpl.setChannelPrototype(_channel);
		channelHubImpl.setCompanyId(companyId);

		return channelHubImpl;
	}

	public void confirmDelivery(
			long userId, Collection<String> notificationEventUuids)
		throws ChannelException {

		confirmDelivery(userId, notificationEventUuids, false);
	}

	public void confirmDelivery(
			long userId, Collection<String> notificationEventUuids,
			boolean archive)
		throws ChannelException {

		Channel channel = getChannel(userId);

		channel.confirmDelivery(notificationEventUuids, archive);
	}

	public void confirmDelivery(long userId, String notificationEventUuid)
		throws ChannelException {

		confirmDelivery(userId, notificationEventUuid, false);
	}

	public void confirmDelivery(
			long userId, String notificationEventUuid, boolean archive)
		throws ChannelException {

		Channel channel = getChannel(userId);

		channel.confirmDelivery(notificationEventUuid, archive);
	}

	public Channel createChannel(long userId) throws ChannelException {
		if (_channels.containsKey(userId)) {
			return _channels.get(userId);
		}

		Channel channel = _channel.clone(_companyId, userId);

		Channel oldChannel = _channels.putIfAbsent(userId, channel);

		if (oldChannel != null) {
			channel.sendNotificationEvents(oldChannel.getNotificationEvents());
		}

		channel.init();

		return channel;
	}

	public void deleteUserNotificiationEvent(
			long userId, String notificationEventUuid)
		throws ChannelException {

		Channel channel = getChannel(userId);

		channel.deleteUserNotificiationEvent(notificationEventUuid);
	}

	public void deleteUserNotificiationEvents(
			long userId, Collection<String> notificationEventUuids)
		throws ChannelException {

		Channel channel = getChannel(userId);

		channel.deleteUserNotificiationEvents(notificationEventUuids);
	}

	public void destroy() throws ChannelException {
		Set<Map.Entry<Long, Channel>> channels = _channels.entrySet();

		Iterator<Map.Entry<Long, Channel>> itr = channels.iterator();

		while (itr.hasNext()) {
			Channel channel = itr.next().getValue();

			channel.close();

			itr.remove();
		}
	}

	public Channel destroyChannel(long userId) throws ChannelException {
		Channel channel = _channels.remove(userId);

		if (channel != null) {
			channel.close();
		}

		return channel;
	}

	public Channel fetchChannel(long userId) throws ChannelException {
		return fetchChannel(userId, false);
	}

	public Channel fetchChannel(long userId, boolean createIfAbsent)
		throws ChannelException {

		Channel channel = _channels.get(userId);

		if (channel == null) {
			synchronized (_channels) {
				channel = _channels.get(userId);

				if (channel == null) {
					if (createIfAbsent) {
						channel = createChannel(userId);
					}
				}
			}
		}

		return channel;
	}

	public void flush() throws ChannelException {
		for (Channel channel : _channels.values()) {
			channel.flush();
		}
	}

	public void flush(long userId) throws ChannelException {
		Channel channel = fetchChannel(userId);

		if (channel != null) {
			channel.flush();
		}
	}

	public void flush(long userId, long timestamp) throws ChannelException {
		Channel channel = fetchChannel(userId);

		if (channel != null) {
			channel.flush(timestamp);
		}
	}

	public Channel getChannel(long userId) throws ChannelException {
		return getChannel(userId, false);
	}

	public Channel getChannel(long userId, boolean createIfAbsent)
		throws ChannelException {

		Channel channel = fetchChannel(userId, createIfAbsent);

		if (channel == null) {
			throw new UnknownChannelException(
				"No channel exists with user id " + userId);
		}

		return channel;
	}

	public long getCompanyId() {
		return _companyId;
	}

	public List<NotificationEvent> getNotificationEvents(long userId)
		throws ChannelException {

		return getNotificationEvents(userId, false);
	}

	public List<NotificationEvent> getNotificationEvents(
			long userId, boolean flush)
		throws ChannelException {

		Channel channel = getChannel(userId);

		return channel.getNotificationEvents(flush);
	}

	public Collection<Long> getUserIds() {
		return Collections.unmodifiableSet(_channels.keySet());
	}

	public void registerChannelListener(
			long userId, ChannelListener channelListener)
		throws ChannelException {

		Channel channel = getChannel(userId);

		channel.registerChannelListener(channelListener);
	}

	public void removeTransientNotificationEvents(
			long userId, Collection<NotificationEvent> notificationEvents)
		throws ChannelException {

		Channel channel = fetchChannel(userId);

		if (channel != null) {
			channel.removeTransientNotificationEvents(notificationEvents);
		}
	}

	public void removeTransientNotificationEventsByUuid(
			long userId, Collection<String> notificationEventUuids)
		throws ChannelException {

		Channel channel = fetchChannel(userId);

		if (channel != null) {
			channel.removeTransientNotificationEventsByUuid(
				notificationEventUuids);
		}
	}

	public void sendNotificationEvent(
			long userId, NotificationEvent notificationEvent)
		throws ChannelException {

		Channel channel = fetchChannel(userId);

		if (channel != null) {
			channel.sendNotificationEvent(notificationEvent);

			return;
		}

		if (!PropsValues.USER_NOTIFICATION_EVENT_CONFIRMATION_ENABLED ||
			!notificationEvent.isDeliveryRequired()) {

			return;
		}

		try {
			UserNotificationEventLocalServiceUtil.addUserNotificationEvent(
				userId, notificationEvent);
		}
		catch (Exception e) {
			throw new ChannelException("Unable to send event", e);
		}
	}

	public void sendNotificationEvents(
			long userId, Collection<NotificationEvent> notificationEvents)
		throws ChannelException {

		Channel channel = fetchChannel(userId);

		if (channel != null) {
			channel.sendNotificationEvents(notificationEvents);

			return;
		}

		if (!PropsValues.USER_NOTIFICATION_EVENT_CONFIRMATION_ENABLED) {
			return;
		}

		List<NotificationEvent> persistedNotificationEvents =
				new ArrayList<NotificationEvent>(notificationEvents.size());

		for (NotificationEvent notificationEvent : notificationEvents) {
			if (notificationEvent.isDeliveryRequired()) {
				persistedNotificationEvents.add(notificationEvent);
			}
		}

		try {
			UserNotificationEventLocalServiceUtil.addUserNotificationEvents(
				userId, persistedNotificationEvents);
		}
		catch (Exception e) {
			throw new ChannelException("Unable to send events", e);
		}
	}

	public void setChannelPrototype(Channel channel) {
		_channel = channel;
	}

	public void setCompanyId(long companyId) {
		_companyId = companyId;
	}

	public void unregisterChannelListener(
			long userId, ChannelListener channelListener)
		throws ChannelException {

		Channel channel = getChannel(userId);

		channel.unregisterChannelListener(channelListener);
	}

	private Channel _channel;
	private ConcurrentMap<Long, Channel> _channels =
		new ConcurrentHashMap<Long, Channel>();
	private long _companyId = CompanyConstants.SYSTEM;

}